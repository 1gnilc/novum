package com.gnilc.novum.session;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 生成并解析带身份命名空间的会话令牌。
 */
final class SessionTokenCodec {
    private static final int RANDOM_BYTES = 32;
    private static final String SEPARATOR = ".";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final String prefix;

    SessionTokenCodec(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix is blank");
        }
        this.prefix = prefix;
    }

    String issue(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId == null");
        }
        byte[] random = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(random);
        return prefix + SEPARATOR + userId + SEPARATOR + ENCODER.encodeToString(random);
    }

    Long resolve(String token) {
        if (!matches(token)) {
            throw new IllegalArgumentException("Invalid session token");
        }
        int first = token.indexOf(SEPARATOR);
        int second = token.indexOf(SEPARATOR, first + 1);
        if (second <= first + 1 || second == token.length() - 1) {
            throw new IllegalArgumentException("Invalid session token");
        }
        try {
            return Long.valueOf(token.substring(first + 1, second));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid session token", e);
        }
    }

    boolean matches(String token) {
        return token != null && token.startsWith(prefix + SEPARATOR);
    }
}
