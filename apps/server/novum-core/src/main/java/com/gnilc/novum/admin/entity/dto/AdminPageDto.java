package com.gnilc.novum.admin.entity.dto;

import com.gnilc.common.utils.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台管理员分页查询条件。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AdminPageDto extends PageParams {
    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 启用状态。
     */
    private Boolean status;
}
