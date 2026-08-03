package com.gnilc.common.exception;

/**
 * 用户凭证未能建立身份。
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
