package com.gnilc.novum.session;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 管理后台管理员会话。
 */
@Service
public class AdminSessionManager {
    private static final SessionPolicy POLICY = new SessionPolicy(
            "sys_admin", "sys:admin", Duration.ofDays(7), Duration.ofDays(30));

    private final SessionEngine sessions;

    @Autowired
    public AdminSessionManager(StringRedisTemplate redis) {
        this(new SessionEngine(POLICY, new SessionRedisStore(redis)));
    }

    AdminSessionManager(SessionEngine sessions) {
        this.sessions = sessions;
    }

    public boolean supportsAccessToken(String token) {
        return sessions.supportsAccessToken(token);
    }

    public SessionTokenPair createSession(Long userId) {
        return sessions.createSession(userId);
    }

    public Long validateAccessToken(String accessToken) {
        return sessions.validateAccessToken(accessToken);
    }

    public SessionTokenPair refreshSession(String refreshToken) {
        return sessions.refreshSession(refreshToken);
    }

    public boolean logout(String refreshToken) {
        return sessions.logout(refreshToken);
    }

    public void cleanupUserSessions(Long userId) {
        sessions.cleanupUserSessions(userId);
    }
}
