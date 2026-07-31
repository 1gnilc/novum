package com.gnilc.novum.admin.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnilc.novum.admin.entity.vo.AdminVo;
import com.gnilc.test.cleanup.RedisCleaner;
import com.gnilc.test.container.RedisContainerContextInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = AdminCacheServiceCacheIT.CacheConfiguration.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = RedisContainerContextInitializer.class)
class AdminCacheServiceCacheIT {
    @Autowired
    private AdminCacheService cache;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private RedisConnectionFactory connectionFactory;

    @BeforeEach
    void cleanRedisBeforeTest() {
        cleanRedis();
    }

    @AfterEach
    void cleanRedisAfterTest() {
        cleanRedis();
    }

    @Test
    void readThroughCacheStoresJsonWithFixedTtlAndReusesIt() {
        AtomicInteger loads = new AtomicInteger();

        List<String> first = cache.getRoleCodes(41L, () -> {
            loads.incrementAndGet();
            return List.of("admin", "rbac:manager");
        });
        Long ttlBeforeHit = redis.getExpire(AdminCacheService.roleCodesKey(41L), TimeUnit.SECONDS);
        List<String> second = cache.getRoleCodes(41L, () -> {
            loads.incrementAndGet();
            return List.of("unexpected");
        });
        Long ttlAfterHit = redis.getExpire(AdminCacheService.roleCodesKey(41L), TimeUnit.SECONDS);

        assertThat(first).containsExactly("admin", "rbac:manager");
        assertThat(second).containsExactly("admin", "rbac:manager");
        assertThat(loads).hasValue(1);
        assertThat(redis.opsForValue().get(AdminCacheService.roleCodesKey(41L)))
                .isEqualTo("[\"admin\",\"rbac:manager\"]");
        assertThat(ttlBeforeHit).isBetween(Duration.ofMinutes(29).getSeconds(), Duration.ofMinutes(30).getSeconds());
        assertThat(ttlAfterHit).isLessThanOrEqualTo(ttlBeforeHit);
    }

    @Test
    void nullUserInfoIsNotStoredAndEmptyListsAreStored() {
        assertThat(cache.getUserInfo(42L, () -> null)).isNull();
        assertThat(cache.getMenuAccessCodes(42L, List::of)).isEmpty();

        assertThat(redis.hasKey(AdminCacheService.userInfoKey(42L))).isFalse();
        assertThat(redis.opsForValue().get(AdminCacheService.menuAccessCodesKey(42L)))
                .isEqualTo("[]");
    }

    @Test
    void globalMenuRemovalsUsePrefixesAndLeaveOtherAdminKeysUntouched() {
        redis.opsForValue().set(AdminCacheService.userInfoKey(43L), "{}");
        redis.opsForValue().set(AdminCacheService.roleCodesKey(43L), "[]");
        redis.opsForValue().set(AdminCacheService.menuAccessCodesKey(43L), "[]");
        redis.opsForValue().set(AdminCacheService.menuRoutesKey(43L), "[]");
        redis.opsForValue().set("sys:admin:at:43:token", "refresh");

        cache.removeAllMenuAccessCodes();
        cache.removeAllMenuRoutes();

        assertThat(redis.hasKey(AdminCacheService.menuAccessCodesKey(43L))).isFalse();
        assertThat(redis.hasKey(AdminCacheService.menuRoutesKey(43L))).isFalse();
        assertThat(redis.hasKey(AdminCacheService.userInfoKey(43L))).isTrue();
        assertThat(redis.hasKey(AdminCacheService.roleCodesKey(43L))).isTrue();
        assertThat(redis.hasKey("sys:admin:at:43:token")).isTrue();
    }

    @Test
    void localStripedLockAllowsOnlyOneDatabaseLoadPerKey() throws Exception {
        int requestCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch loaderStarted = new CountDownLatch(1);
        CountDownLatch releaseLoader = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        List<CompletableFuture<List<String>>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < requestCount; index++) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    ready.countDown();
                    awaitLatch(start);
                    return cache.getRoleCodes(44L, () -> {
                        loads.incrementAndGet();
                        loaderStarted.countDown();
                        awaitLatch(releaseLoader);
                        return List.of("admin");
                    });
                }, executor));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(loaderStarted.await(5, TimeUnit.SECONDS)).isTrue();
            releaseLoader.countDown();

            for (CompletableFuture<List<String>> future : futures) {
                assertThat(future.get(5, TimeUnit.SECONDS)).containsExactly("admin");
            }
            assertThat(loads).hasValue(1);
        } finally {
            releaseLoader.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void delayedSecondDeleteRunsAfterFiveSeconds() {
        AdminVo userInfo = new AdminVo();
        userInfo.setUserId(45L);
        userInfo.setNickname("Cached Admin");
        cache.getUserInfo(45L, () -> userInfo);

        cache.scheduleSecondDelete(() -> cache.removeUserInfo(45L));

        assertThat(redis.hasKey(AdminCacheService.userInfoKey(45L))).isTrue();
        await().atMost(Duration.ofSeconds(7)).untilAsserted(() ->
                assertThat(redis.hasKey(AdminCacheService.userInfoKey(45L))).isFalse());
    }

    private void cleanRedis() {
        new RedisCleaner(connectionFactory).flushDatabase();
    }

    private static void awaitLatch(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent cache test latch.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent cache test latch.", exception);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(AdminCacheService.class)
    @ImportAutoConfiguration({RedisAutoConfiguration.class, JacksonAutoConfiguration.class})
    static class CacheConfiguration {
    }
}
