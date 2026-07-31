package com.gnilc.novum.admin.entity.vo;

import lombok.Data;

/**
 * 后台管理员登录令牌。
 */
@Data
public class AdminTokenVo {
    /**
     * Bearer 访问令牌。
     */
    private String accessToken;

    /**
     * 刷新令牌。
     */
    private String refreshToken;

    /**
     * 创建令牌响应。
     */
    public static AdminTokenVo of(String accessToken, String refreshToken) {
        AdminTokenVo vo = new AdminTokenVo();
        vo.setAccessToken(accessToken);
        vo.setRefreshToken(refreshToken);
        return vo;
    }
}
