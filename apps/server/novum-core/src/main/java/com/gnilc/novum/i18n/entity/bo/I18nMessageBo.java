package com.gnilc.novum.i18n.entity.bo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 动态国际化消息表映射。
 *
 * <p>每条记录表示某个消息 key 的单语言翻译。</p>
 */
@Data
@TableName("sys_i18n")
public class I18nMessageBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 消息分类，用于组织消息并限定运行时语言包范围。
     */
    private String category;

    /**
     * 国际化消息 key，使用点分路径表示消息层级。
     */
    private String messageKey;

    /**
     * 语言代码，例如 zh-CN、en-US。
     */
    private String locale;

    /**
     * 当前消息 key 和语言对应的翻译值。
     */
    private String i18nValue;

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
}
