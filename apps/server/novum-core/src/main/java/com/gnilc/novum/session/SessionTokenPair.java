package com.gnilc.novum.session;

import lombok.Getter;

/**
 * 会话访问令牌和刷新令牌。
 */
@Getter
public final class SessionTokenPair {
    private final String accessToken;
    private final String refreshToken;

    private SessionTokenPair(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static SessionTokenPair of(String accessToken, String refreshToken) {
        return new SessionTokenPair(accessToken, refreshToken);
    }
}
