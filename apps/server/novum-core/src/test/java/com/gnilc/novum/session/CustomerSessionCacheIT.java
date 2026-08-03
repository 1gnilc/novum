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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
class CustomerSessionCacheIT {
    private static final Duration ACCESS_TTL = Duration.ofDays(7);
    private static final Duration REFRESH_TTL = Duration.ofDays(30);

    @Autowired private AdminSessionManager adminSessions;
    @Autowired private CustomerSessionManager customerSessions;
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

    @Test
    void sessionLifecyclePersistsTtlRefreshesAndRevokesTokensInRedis8() {
        SessionTokenPair first = customerSessions.createSession(33L);
        String accessKey = accessKey(33L, first.getAccessToken());
        String refreshKey = refreshKey(33L, first.getRefreshToken());

        assertThat(first.getAccessToken()).startsWith("customer.33.");
        assertThat(first.getRefreshToken()).startsWith("customer.33.");
        assertThat(redis.opsForValue().get(accessKey)).isEqualTo(first.getRefreshToken());
        assertThat(redis.opsForValue().get(refreshKey)).isEqualTo(first.getAccessToken());
        assertTtlNear(accessKey, ACCESS_TTL);
        assertTtlNear(refreshKey, REFRESH_TTL);
        Long refreshTtlBefore = redis.getExpire(refreshKey, TimeUnit.SECONDS);

        SessionTokenPair refreshed = customerSessions.refreshSession(first.getRefreshToken());

        assertThat(refreshed).isNotNull();
        assertThat(refreshed.getAccessToken()).isNotEqualTo(first.getAccessToken());
        assertThat(refreshed.getRefreshToken()).isEqualTo(first.getRefreshToken());
        assertThat(customerSessions.validateAccessToken(first.getAccessToken())).isNull();
        assertThat(redis.hasKey(accessKey)).isFalse();
        assertThat(redis.opsForValue().get(accessKey(33L, refreshed.getAccessToken())))
                .isEqualTo(first.getRefreshToken());
        assertThat(redis.opsForValue().get(refreshKey)).isEqualTo(refreshed.getAccessToken());
        assertThat(redis.getExpire(refreshKey, TimeUnit.SECONDS))
                .isBetween(refreshTtlBefore - 2, refreshTtlBefore);

        assertThat(customerSessions.logout(refreshed.getRefreshToken())).isTrue();
        assertThat(customerSessions.validateAccessToken(refreshed.getAccessToken())).isNull();
        assertThat(redis.hasKey(accessKey(33L, refreshed.getAccessToken()))).isFalse();
        assertThat(redis.hasKey(refreshKey)).isFalse();
    }

    @Test
    void customerAndAdminSessionsUseSeparateTokenAndRedisNamespaces() {
        SessionTokenPair customer = customerSessions.createSession(41L);
        SessionTokenPair admin = adminSessions.createSession(41L);

        assertThat(customerSessions.validateAccessToken(customer.getAccessToken())).isEqualTo(41L);
        assertThat(customerSessions.validateAccessToken(admin.getAccessToken())).isNull();
        assertThat(adminSessions.validateAccessToken(admin.getAccessToken())).isEqualTo(41L);
        assertThat(adminSessions.validateAccessToken(customer.getAccessToken())).isNull();
        assertThat(redis.hasKey(accessKey(41L, customer.getAccessToken()))).isTrue();
        assertThat(redis.hasKey("sys:admin:at:41:" + admin.getAccessToken())).isTrue();
    }

    private void cleanRedis() {
        new RedisCleaner(connectionFactory).flushDatabase();
    }

    private String accessKey(Long userId, String token) {
        return "customer:at:" + userId + ":" + token;
    }

    private String refreshKey(Long userId, String token) {
        return "customer:rt:" + userId + ":" + token;
    }

    private void assertTtlNear(String key, Duration expected) {
        Long actualSeconds = redis.getExpire(key, TimeUnit.SECONDS);
        assertThat(actualSeconds).isNotNull();
        assertThat(actualSeconds).isPositive();
        assertThat(actualSeconds).isGreaterThan(expected.minusSeconds(10).getSeconds());
        assertThat(actualSeconds).isLessThanOrEqualTo(expected.getSeconds());
    }
}
