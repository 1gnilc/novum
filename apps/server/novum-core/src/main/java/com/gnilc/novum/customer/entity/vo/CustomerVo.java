package com.gnilc.novum.customer.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gnilc.novum.image.annotation.ObjectKeyToUrl;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * 当前 Customer 信息。
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerVo {
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createTime;

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    @ObjectKeyToUrl("avatar")
    private String avatarUrl;
    private List<String> roleCodes;
}
