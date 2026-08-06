package com.gnilc.novum.image.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gnilc.novum.image.entity.enums.ImageStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class ImageVo {
    private Long id;
    private String objectKey;
    private String url;
    private String contentType;
    private Long contentLength;
    private ImageStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createTime;
}
