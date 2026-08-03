package com.gnilc.novum.session;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 管理后台管理员会话。
 */
@Service
public class AdminSessionManager implements SessionManager {
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

    @Override
    public boolean supportsAccessToken(String token) {
        return sessions.supportsAccessToken(token);
    }

    @Override
    public SessionTokenPair createSession(Long userId) {
        return sessions.createSession(userId);
    }

    @Override
    public Long validateAccessToken(String accessToken) {
        return sessions.validateAccessToken(accessToken);
    }

    @Override
    public SessionTokenPair refreshSession(String refreshToken) {
        return sessions.refreshSession(refreshToken);
    }

    @Override
    public boolean logout(String refreshToken) {
        return sessions.logout(refreshToken);
    }

    @Override
    public void cleanupUserSessions(Long userId) {
        sessions.cleanupUserSessions(userId);
    }
}
