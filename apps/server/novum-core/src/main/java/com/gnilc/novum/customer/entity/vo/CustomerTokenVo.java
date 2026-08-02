package com.gnilc.novum.customer.entity.vo;

import lombok.Data;

/**
 * Customer 登录令牌。
 */
@Data
public class CustomerTokenVo {
    private String accessToken;
    private String refreshToken;

    public static CustomerTokenVo of(String accessToken, String refreshToken) {
        CustomerTokenVo token = new CustomerTokenVo();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        return token;
    }
}
