package com.gnilc.novum.customer.entity.bo;

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
 * Customer 表映射。
 */
@Data
@TableName("nv_customer")
public class CustomerBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer del;

    @TableField(fill = FieldFill.INSERT)
    private Instant createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Instant updateTime;

    private Long userId;
    private String username;
    private String password;
    private String nickname;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String avatar;

    private Boolean status;
}
