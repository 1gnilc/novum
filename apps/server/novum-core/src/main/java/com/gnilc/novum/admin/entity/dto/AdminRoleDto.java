package com.gnilc.novum.admin.entity.dto;

import lombok.Data;

import java.util.List;

/**
 * 后台管理员角色替换请求。
 */
@Data
public class AdminRoleDto {
    /**
     * 管理员 ID。
     */
    private Long id;

    /**
     * 目标角色标识；null 或空列表清空。
     */
    private List<String> roleCodes;
}
