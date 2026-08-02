package com.gnilc.novum.session;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

/**
 * 封装会话令牌的 Redis 读写。
 */
final class SessionRedisStore {
    private static final DefaultRedisScript<Long> ROTATE_ACCESS_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            local paired_access_token = redis.call('GET', KEYS[1])
            if paired_access_token ~= ARGV[1] then
                return 0
            end
            redis.call('SET', KEYS[1], ARGV[2], 'KEEPTTL')
            redis.call('DEL', KEYS[2])
            redis.call('SET', KEYS[3], ARGV[3], 'EX', ARGV[4])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> DELETE_SESSION_SCRIPT = new DefaultRedisScript<>("""
            local paired_access_token = redis.call('GET', KEYS[1])
            if not paired_access_token then
                return 0
            end
            redis.call('DEL', ARGV[1] .. paired_access_token)
            redis.call('DEL', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;

    SessionRedisStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    void saveSession(SessionPolicy policy, Long userId, String accessToken, String refreshToken) {
        redis.opsForValue().set(
                policy.accessKey(userId, accessToken), refreshToken, policy.accessTtl());
        redis.opsForValue().set(
                policy.refreshKey(userId, refreshToken), accessToken, policy.refreshTtl());
    }

    boolean hasAccessToken(SessionPolicy policy, Long userId, String accessToken) {
        return redis.hasKey(policy.accessKey(userId, accessToken));
    }

    boolean hasRefreshToken(SessionPolicy policy, Long userId, String refreshToken) {
        return redis.hasKey(policy.refreshKey(userId, refreshToken));
    }

    String getPairedAccessToken(SessionPolicy policy, Long userId, String refreshToken) {
        return redis.opsForValue().get(policy.refreshKey(userId, refreshToken));
    }

    boolean rotateAccessToken(SessionPolicy policy, Long userId, String refreshToken,
                              String oldAccessToken, String newAccessToken) {
        Long result = redis.execute(
                ROTATE_ACCESS_TOKEN_SCRIPT,
                List.of(
                        policy.refreshKey(userId, refreshToken),
                        policy.accessKey(userId, oldAccessToken),
                        policy.accessKey(userId, newAccessToken)),
                oldAccessToken,
                newAccessToken,
                refreshToken,
                Long.toString(policy.accessTtl().toSeconds()));
        return Long.valueOf(1L).equals(result);
    }

    boolean deleteSession(SessionPolicy policy, Long userId, String refreshToken) {
        Long result = redis.execute(
                DELETE_SESSION_SCRIPT,
                List.of(policy.refreshKey(userId, refreshToken)),
                policy.accessKeyPrefix(userId));
        return Long.valueOf(1L).equals(result);
    }

    void deleteUserSessions(SessionPolicy policy, Long userId) {
        deleteKeys(redis.keys(policy.accessPattern(userId)));
        deleteKeys(redis.keys(policy.refreshPattern(userId)));
    }

    private void deleteKeys(Set<String> keys) {
        if (!CollectionUtils.isEmpty(keys)) {
            redis.delete(keys);
        }
    }
}
