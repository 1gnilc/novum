package com.gnilc.novum.admin.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

import java.util.List;

/**
 * 后台管理员创建/更新请求。
 */
@Data
public class AdminDto {
    /**
     * 管理员 ID，更新时必填。
     */
    private Long id;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * 明文密码，用于创建或改密。
     */
    private String password;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 头像地址。
     */
    private String avatar;

    @JsonIgnore
    private boolean avatarSpecified;

    /**
     * 管理员描述。
     */
    private String desc;

    @JsonIgnore
    private boolean descSpecified;

    /**
     * 默认首页路径。
     */
    private String homePath;

    /**
     * 启用状态。
     */
    private Boolean status;

    /**
     * 角色标识；null 不变，空列表清空。
     */
    private List<String> roleCodes;

    @JsonSetter
    public void setAvatar(String avatar) {
        this.avatar = avatar;
        this.avatarSpecified = true;
    }

    @JsonSetter
    public void setDesc(String desc) {
        this.desc = desc;
        this.descSpecified = true;
    }
}
