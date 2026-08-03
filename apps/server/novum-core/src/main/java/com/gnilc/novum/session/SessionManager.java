package com.gnilc.novum.session;

/**
 * 定义身份会话管理器的公共能力。
 */
public interface SessionManager {

    boolean supportsAccessToken(String token);

    SessionTokenPair createSession(Long userId);

    Long validateAccessToken(String accessToken);

    SessionTokenPair refreshSession(String refreshToken);

    boolean logout(String refreshToken);

    void cleanupUserSessions(Long userId);
}
