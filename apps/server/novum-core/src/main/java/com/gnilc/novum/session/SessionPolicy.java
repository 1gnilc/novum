package com.gnilc.novum.session;

import lombok.Data;

import java.time.Duration;

/**
 * 定义一种身份会话的令牌和 Redis 策略。
 */
@Data
final class SessionPolicy {
    private final String tokenPrefix;
    private final String redisNamespace;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    SessionPolicy(String tokenPrefix, String redisNamespace, Duration accessTtl, Duration refreshTtl) {
        if (tokenPrefix == null || tokenPrefix.isBlank()) {
            throw new IllegalArgumentException("tokenPrefix is blank");
        }
        if (redisNamespace == null || redisNamespace.isBlank()) {
            throw new IllegalArgumentException("redisNamespace is blank");
        }
        if (accessTtl == null || accessTtl.isNegative() || accessTtl.isZero()) {
            throw new IllegalArgumentException("accessTtl must be positive");
        }
        if (refreshTtl == null || refreshTtl.isNegative() || refreshTtl.isZero()) {
            throw new IllegalArgumentException("refreshTtl must be positive");
        }
        this.tokenPrefix = tokenPrefix;
        this.redisNamespace = redisNamespace;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    String accessKey(Long userId, String token) {
        return accessKeyPrefix(userId) + token;
    }

    String accessKeyPrefix(Long userId) {
        return redisNamespace + ":at:" + userId + ":";
    }

    String accessPattern(Long userId) {
        return accessKeyPrefix(userId) + "*";
    }

    String refreshKey(Long userId, String token) {
        return redisNamespace + ":rt:" + userId + ":" + token;
    }

    String refreshPattern(Long userId) {
        return redisNamespace + ":rt:" + userId + ":*";
    }
}
