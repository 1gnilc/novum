package com.gnilc.novum.session;

import lombok.Getter;

/**
 * 后台管理员会话令牌对。
 */
@Getter
public final class AdminSessionTokenPair {
    private final String accessToken;
    private final String refreshToken;

    private AdminSessionTokenPair(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    /**
     * 创建令牌对。
     */
    public static AdminSessionTokenPair of(String accessToken, String refreshToken) {
        return new AdminSessionTokenPair(accessToken, refreshToken);
    }

}
