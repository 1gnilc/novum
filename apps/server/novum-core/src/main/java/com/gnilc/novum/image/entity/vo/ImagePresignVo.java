package com.gnilc.novum.image.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
public class ImagePresignVo {
    private String objectKey;
    private String uploadUrl;
    private String method;
    private Map<String, String> headers;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant expiresAt;
}
