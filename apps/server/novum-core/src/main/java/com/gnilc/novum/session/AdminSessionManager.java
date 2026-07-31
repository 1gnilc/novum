package com.gnilc.novum.session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 管理后台管理员会话。
 */
@Service
public class AdminSessionManager {
    private final AdminSessionTokenCodec tokenCodec;
    private final AdminSessionRedisCommands redisCommands;

    @Autowired
    public AdminSessionManager(AdminSessionRedisCommands redisCommands) {
        this(redisCommands, new AdminSessionTokenCodec());
    }

    AdminSessionManager(AdminSessionRedisCommands redisCommands, AdminSessionTokenCodec tokenCodec) {
        this.redisCommands = redisCommands;
        this.tokenCodec = tokenCodec;
    }

    /**
     * 判断是否为后台管理员访问令牌。
     */
    public boolean supportsAccessToken(String token) {
        return tokenCodec.matches(token);
    }

    /**
     * 创建访问令牌和刷新令牌。
     */
    public AdminSessionTokenPair createSession(Long userId) {
        String accessToken = tokenCodec.issue(userId);
        String refreshToken = tokenCodec.issue(userId);
        redisCommands.saveSession(userId, accessToken, refreshToken);
        return AdminSessionTokenPair.of(accessToken, refreshToken);
    }

    /**
     * 校验访问令牌。
     */
    public Long validateAccessToken(String accessToken) {
        Long userId = parseUserId(accessToken);
        if (userId == null) {
            return null;
        }
        return redisCommands.hasAccessToken(userId, accessToken) ? userId : null;
    }

    /**
     * 刷新访问令牌。
     */
    public AdminSessionTokenPair refreshSession(String refreshToken) {
        Long userId = validateRefreshToken(refreshToken);
        if (userId == null) {
            return null;
        }
        String oldAccessToken = redisCommands.getPairedAccessToken(userId, refreshToken);
        if (oldAccessToken == null || oldAccessToken.isBlank()) {
            return null;
        }
        String accessToken = tokenCodec.issue(userId);
        if (!redisCommands.rotateAccessToken(
                userId, refreshToken, oldAccessToken, accessToken)) {
            return null;
        }
        return AdminSessionTokenPair.of(accessToken, refreshToken);
    }

    /**
     * 登出当前会话。
     */
    public boolean logout(String refreshToken) {
        Long userId = parseUserId(refreshToken);
        if (userId == null) {
            return false;
        }
        return redisCommands.deleteSession(userId, refreshToken);
    }

    /**
     * 清理用户全部会话。
     */
    public void cleanupUserSessions(Long userId) {
        redisCommands.deleteUserSessions(userId);
    }

    /**
     * 校验刷新令牌。
     */
    private Long validateRefreshToken(String refreshToken) {
        Long userId = parseUserId(refreshToken);
        if (userId == null) {
            return null;
        }
        return redisCommands.hasRefreshToken(userId, refreshToken) ? userId : null;
    }

    /**
     * 解析令牌中的 user_id。
     */
    private Long parseUserId(String token) {
        try {
            return tokenCodec.resolve(token);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
