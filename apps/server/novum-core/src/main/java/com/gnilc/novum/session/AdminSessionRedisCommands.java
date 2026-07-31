package com.gnilc.novum.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * 封装后台管理员会话的 Redis 读写。
 */
@Component
public class AdminSessionRedisCommands {
    private static final String ACCESS_PREFIX = "sys:admin:at:";
    private static final String REFRESH_PREFIX = "sys:admin:rt:";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofDays(7);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);
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

    private final StringRedisTemplate redisTemplate;

    @Autowired
    public AdminSessionRedisCommands(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存访问令牌和刷新令牌。
     */
    void saveSession(Long userId, String accessToken, String refreshToken) {
        saveAccessToken(userId, accessToken, refreshToken);
        redisTemplate.opsForValue().set(refreshKey(userId, refreshToken), accessToken, REFRESH_TOKEN_TTL);
    }

    /**
     * 保存访问令牌映射。
     */
    private void saveAccessToken(Long userId, String accessToken, String refreshToken) {
        redisTemplate.opsForValue().set(accessKey(userId, accessToken), refreshToken, ACCESS_TOKEN_TTL);
    }

    /**
     * 判断访问令牌是否存在。
     */
    boolean hasAccessToken(Long userId, String accessToken) {
        return redisTemplate.hasKey(accessKey(userId, accessToken));
    }

    /**
     * 判断刷新令牌是否存在。
     */
    boolean hasRefreshToken(Long userId, String refreshToken) {
        return redisTemplate.hasKey(refreshKey(userId, refreshToken));
    }

    /**
     * 读取刷新令牌绑定的访问令牌。
     */
    String getPairedAccessToken(Long userId, String refreshToken) {
        return redisTemplate.opsForValue().get(refreshKey(userId, refreshToken));
    }

    /**
     * 替换刷新令牌绑定的访问令牌并保留 TTL。
     */
    boolean rotateAccessToken(Long userId, String refreshToken,
                              String oldAccessToken, String newAccessToken) {
        Long result = redisTemplate.execute(
                ROTATE_ACCESS_TOKEN_SCRIPT,
                List.of(
                        refreshKey(userId, refreshToken),
                        accessKey(userId, oldAccessToken),
                        accessKey(userId, newAccessToken)),
                oldAccessToken,
                newAccessToken,
                refreshToken,
                Long.toString(ACCESS_TOKEN_TTL.toSeconds()));
        return Long.valueOf(1L).equals(result);
    }

    /**
     * 原子删除刷新令牌及其当前绑定的访问令牌。
     */
    boolean deleteSession(Long userId, String refreshToken) {
        Long result = redisTemplate.execute(
                DELETE_SESSION_SCRIPT,
                List.of(refreshKey(userId, refreshToken)),
                accessKeyPrefix(userId));
        return Long.valueOf(1L).equals(result);
    }

    /**
     * 删除用户全部会话令牌。
     */
    void deleteUserSessions(Long userId) {
        deleteKeys(redisTemplate.keys(accessPattern(userId)));
        deleteKeys(redisTemplate.keys(refreshPattern(userId)));
    }

    /**
     * 构造访问令牌 key。
     */
    String accessKey(Long userId, String accessToken) {
        return accessKeyPrefix(userId) + accessToken;
    }

    /**
     * 构造刷新令牌 key。
     */
    String refreshKey(Long userId, String refreshToken) {
        return REFRESH_PREFIX + userId + ":" + refreshToken;
    }

    /**
     * 构造访问令牌清理 pattern。
     */
    String accessPattern(Long userId) {
        return accessKeyPrefix(userId) + "*";
    }

    /**
     * 构造刷新令牌清理 pattern。
     */
    String refreshPattern(Long userId) {
        return REFRESH_PREFIX + userId + ":*";
    }

    /**
     * 批量删除 key。
     */
    private void deleteKeys(Set<String> keys) {
        if (!CollectionUtils.isEmpty(keys)) {
            redisTemplate.delete(keys);
        }
    }

    private String accessKeyPrefix(Long userId) {
        return ACCESS_PREFIX + userId + ":";
    }
}
