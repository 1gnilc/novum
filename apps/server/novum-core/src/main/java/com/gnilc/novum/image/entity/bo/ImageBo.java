package com.gnilc.novum.image.entity.bo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gnilc.novum.image.entity.enums.ImageStatus;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 托管图片表映射。
 */
@Data
@TableName("sys_image")
public class ImageBo implements Serializable {
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
     * S3 对象键，作为图片在存储中的稳定标识。
     */
    private String objectKey;

    /**
     * 上传时声明的媒体类型，用于完成上传时校验对象元数据。
     */
    private String contentType;

    /**
     * 上传时声明的文件大小，单位为字节。
     */
    private Long contentLength;

    /**
     * 图片上传生命周期状态。
     */
    private ImageStatus status;

    /**
     * 待上传记录的过期时间；完成上传后清空。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Instant expiresAt;
}
