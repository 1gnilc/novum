package com.gnilc.novum.session;

import java.time.Duration;

/**
 * 定义一种身份会话的令牌和 Redis 策略。
 */
record SessionPolicy(
        String tokenPrefix,
        String redisNamespace,
        Duration accessTtl,
        Duration refreshTtl) {

    SessionPolicy {
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
