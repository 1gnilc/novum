package com.gnilc.novum.session;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 生成并解析后台管理员会话令牌。
 */
final class AdminSessionTokenCodec {
    private static final int RANDOM_BYTES = 32;
    private static final String TOKEN_PREFIX = "sys_admin";
    private static final String TOKEN_SEPARATOR = ".";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    /**
     * 签发后台管理员会话令牌。
     */
    String issue(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId == null");
        }
        byte[] random = new byte[RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(random);
        return TOKEN_PREFIX + TOKEN_SEPARATOR + userId + TOKEN_SEPARATOR + ENCODER.encodeToString(random);
    }

    /**
     * 解析令牌中的 user_id。
     */
    Long resolve(String token) {
        if (!matches(token)) {
            throw new IllegalArgumentException("Invalid admin token");
        }
        int firstSeparator = token.indexOf(TOKEN_SEPARATOR);
        int secondSeparator = token.indexOf(TOKEN_SEPARATOR, firstSeparator + 1);
        if (secondSeparator <= firstSeparator + 1 || secondSeparator == token.length() - 1) {
            throw new IllegalArgumentException("Invalid admin token");
        }
        try {
            return Long.valueOf(token.substring(firstSeparator + 1, secondSeparator));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid admin token", e);
        }
    }

    /**
     * 判断令牌命名空间。
     */
    boolean matches(String token) {
        return token != null && token.startsWith(TOKEN_PREFIX + TOKEN_SEPARATOR);
    }
}
