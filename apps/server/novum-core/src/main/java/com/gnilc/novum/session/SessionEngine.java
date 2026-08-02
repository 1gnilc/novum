package com.gnilc.novum.session;

/**
 * 管理一种身份会话的令牌生命周期。
 */
final class SessionEngine {
    private final SessionPolicy policy;
    private final SessionRedisStore store;
    private final SessionTokenCodec codec;

    SessionEngine(SessionPolicy policy, SessionRedisStore store) {
        this.policy = policy;
        this.store = store;
        this.codec = new SessionTokenCodec(policy.tokenPrefix());
    }

    boolean supportsAccessToken(String token) {
        return codec.matches(token);
    }

    SessionTokenPair createSession(Long userId) {
        String accessToken = codec.issue(userId);
        String refreshToken = codec.issue(userId);
        store.saveSession(policy, userId, accessToken, refreshToken);
        return SessionTokenPair.of(accessToken, refreshToken);
    }

    Long validateAccessToken(String accessToken) {
        Long userId = parseUserId(accessToken);
        if (userId == null) {
            return null;
        }
        return store.hasAccessToken(policy, userId, accessToken) ? userId : null;
    }

    SessionTokenPair refreshSession(String refreshToken) {
        Long userId = validateRefreshToken(refreshToken);
        if (userId == null) {
            return null;
        }
        String oldAccessToken = store.getPairedAccessToken(policy, userId, refreshToken);
        if (oldAccessToken == null || oldAccessToken.isBlank()) {
            return null;
        }
        String accessToken = codec.issue(userId);
        if (!store.rotateAccessToken(
                policy, userId, refreshToken, oldAccessToken, accessToken)) {
            return null;
        }
        return SessionTokenPair.of(accessToken, refreshToken);
    }

    boolean logout(String refreshToken) {
        Long userId = parseUserId(refreshToken);
        return userId != null && store.deleteSession(policy, userId, refreshToken);
    }

    void cleanupUserSessions(Long userId) {
        store.deleteUserSessions(policy, userId);
    }

    private Long validateRefreshToken(String refreshToken) {
        Long userId = parseUserId(refreshToken);
        if (userId == null) {
            return null;
        }
        return store.hasRefreshToken(policy, userId, refreshToken) ? userId : null;
    }

    private Long parseUserId(String token) {
        try {
            return codec.resolve(token);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
