package com.gnilc.novum.admin.entity.bo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 后台管理员表映射。
 */
@Data
@TableName("sys_admin")
public class AdminBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 删除标记。
     */
    private Integer del;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;

    /**
     * 更新时间。
     */
    @TableField(fill = FieldFill.UPDATE)
    private Instant updateTime;

    /**
     * RBAC 全局用户 ID。
     */
    private Long userId;

    /**
     * 登录用户名。
     */
    private String username;

    /**
     * BCrypt 密码哈希。
     */
    private String password;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 头像地址。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String avatar;

    /**
     * 管理员描述。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    /**
     * 默认首页路径。
     */
    private String homePath;

    /**
     * 启用状态。
     */
    private Boolean status;
}
