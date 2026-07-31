package com.gnilc.novum.session;

import com.gnilc.novum.support.SystemContainerContextInitializer;
import com.gnilc.novum.support.SystemTestApplication;
import com.gnilc.test.cleanup.RedisCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
class AdminSessionCacheIT {
    private static final Duration ACCESS_TTL = Duration.ofDays(7);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    @Autowired private AdminSessionManager sessions;
    @Autowired private AdminSessionRedisCommands commands;
    @Autowired private StringRedisTemplate redis;
    @Autowired private RedisConnectionFactory connectionFactory;

    @BeforeEach
    void cleanRedisBeforeTest() {
        cleanRedis();
    }

    @AfterEach
    void cleanRedisAfterTest() {
        cleanRedis();
    }

    private void cleanRedis() {
        new RedisCleaner(connectionFactory).flushDatabase();
    }

    @Test
    void sessionLifecyclePersistsTtlRefreshesAndRevokesTokensInRedis8() {
        AdminSessionTokenPair first = sessions.createSession(33L);
        String firstAccessKey = commands.accessKey(33L, first.getAccessToken());
        String refreshKey = commands.refreshKey(33L, first.getRefreshToken());

        assertThat(sessions.validateAccessToken(first.getAccessToken())).isEqualTo(33L);
        assertThat(redis.opsForValue().get(firstAccessKey)).isEqualTo(first.getRefreshToken());
        assertThat(redis.opsForValue().get(refreshKey)).isEqualTo(first.getAccessToken());
        assertTtlNear(firstAccessKey, ACCESS_TTL);
        assertTtlNear(refreshKey, REFRESH_TTL);
        Long refreshTtlBefore = redis.getExpire(refreshKey, TimeUnit.SECONDS);

        AdminSessionTokenPair refreshed = sessions.refreshSession(first.getRefreshToken());
        assertThat(refreshed.getAccessToken()).isNotEqualTo(first.getAccessToken());
        assertThat(refreshed.getRefreshToken()).isEqualTo(first.getRefreshToken());
        assertThat(sessions.validateAccessToken(first.getAccessToken())).isNull();
        assertThat(sessions.validateAccessToken(refreshed.getAccessToken())).isEqualTo(33L);
        assertThat(redis.hasKey(firstAccessKey)).isFalse();
        assertThat(redis.opsForValue().get(commands.accessKey(33L, refreshed.getAccessToken())))
                .isEqualTo(first.getRefreshToken());
        assertThat(redis.opsForValue().get(refreshKey)).isEqualTo(refreshed.getAccessToken());
        assertThat(redis.getExpire(refreshKey, TimeUnit.SECONDS))
                .isBetween(refreshTtlBefore - 2, refreshTtlBefore);

        assertThat(sessions.logout(refreshed.getRefreshToken())).isTrue();
        assertThat(sessions.validateAccessToken(refreshed.getAccessToken())).isNull();
        assertThat(sessions.refreshSession(refreshed.getRefreshToken())).isNull();
    }

    @Test
    void rotatingADeletedRefreshKeyDoesNotRecreateIt() {
        String refreshKey = commands.refreshKey(42L, "missing-refresh-token");

        boolean replaced = commands.rotateAccessToken(
                42L, "missing-refresh-token", "old-access-token", "replacement-access-token");

        assertThat(replaced).isFalse();
        assertThat(redis.hasKey(refreshKey)).isFalse();
        assertThat(redis.opsForValue().get(refreshKey)).isNull();
    }

    @Test
    void concurrentRefreshLeavesOnlyOneValidNewAccessToken() {
        int requestCount = 16;
        AdminSessionTokenPair session = sessions.createSession(43L);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<AdminSessionTokenPair>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    ready.countDown();
                    await(ready);
                    await(start);
                    return sessions.refreshSession(session.getRefreshToken());
                }, executor));
            }
            start.countDown();

            List<AdminSessionTokenPair> successfulRefreshes = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            List<AdminSessionTokenPair> validRefreshes = successfulRefreshes.stream()
                    .filter(refreshed -> sessions.validateAccessToken(refreshed.getAccessToken()) != null)
                    .toList();

            assertThat(successfulRefreshes).isNotEmpty();
            assertThat(sessions.validateAccessToken(session.getAccessToken())).isNull();
            assertThat(validRefreshes).singleElement().satisfies(refreshed -> {
                assertThat(refreshed.getRefreshToken()).isEqualTo(session.getRefreshToken());
                assertThat(sessions.validateAccessToken(refreshed.getAccessToken())).isEqualTo(43L);
                assertThat(redis.opsForValue().get(commands.refreshKey(43L, session.getRefreshToken())))
                        .isEqualTo(refreshed.getAccessToken());
                assertThat(redis.keys(commands.accessPattern(43L)))
                        .containsExactly(commands.accessKey(43L, refreshed.getAccessToken()));
            });
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentRefreshAndLogoutLeaveNoValidAccessToken() {
        int sessionCount = 64;
        List<AdminSessionTokenPair> initialSessions = new ArrayList<>();
        for (int i = 0; i < sessionCount; i++) {
            initialSessions.add(sessions.createSession(44L));
        }
        ExecutorService executor = Executors.newFixedThreadPool(sessionCount * 2);
        CountDownLatch ready = new CountDownLatch(sessionCount * 2);
        CountDownLatch start = new CountDownLatch(1);
        List<CompletableFuture<AdminSessionTokenPair>> refreshes = new ArrayList<>();
        List<CompletableFuture<Boolean>> logouts = new ArrayList<>();

        try {
            for (AdminSessionTokenPair session : initialSessions) {
                refreshes.add(CompletableFuture.supplyAsync(() -> {
                    ready.countDown();
                    await(ready);
                    await(start);
                    return sessions.refreshSession(session.getRefreshToken());
                }, executor));
                logouts.add(CompletableFuture.supplyAsync(() -> {
                    ready.countDown();
                    await(ready);
                    await(start);
                    return sessions.logout(session.getRefreshToken());
                }, executor));
            }
            start.countDown();

            List<AdminSessionTokenPair> refreshedSessions = refreshes.stream()
                    .map(CompletableFuture::join)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            assertThat(logouts).allSatisfy(future -> assertThat(future.join()).isTrue());
            assertThat(initialSessions).allSatisfy(session ->
                    assertThat(sessions.validateAccessToken(session.getAccessToken())).isNull());
            assertThat(refreshedSessions).allSatisfy(session ->
                    assertThat(sessions.validateAccessToken(session.getAccessToken())).isNull());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void cleanupUserSessionsLeavesOtherUsersUntouched() {
        AdminSessionTokenPair user = sessions.createSession(40L);
        AdminSessionTokenPair other = sessions.createSession(41L);

        sessions.cleanupUserSessions(40L);

        assertThat(sessions.validateAccessToken(user.getAccessToken())).isNull();
        assertThat(sessions.validateAccessToken(other.getAccessToken())).isEqualTo(41L);
        assertThat(redis.hasKey(commands.refreshKey(41L, other.getRefreshToken()))).isTrue();
        assertThat(redis.keys("sys:admin:*:40:*")).isEmpty();
    }

    private void assertTtlNear(String key, Duration expected) {
        Long actualSeconds = redis.getExpire(key, TimeUnit.SECONDS);
        assertThat(actualSeconds).isNotNull();
        assertThat(actualSeconds).isPositive();
        assertThat(actualSeconds).isGreaterThan(expected.minusSeconds(10).getSeconds());
        assertThat(actualSeconds).isLessThanOrEqualTo(expected.getSeconds());
    }

    private static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for concurrent refresh", e);
        }
    }
}
