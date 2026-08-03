package com.gnilc.common.exception;

/**
 * 请求没有可用的认证会话。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
