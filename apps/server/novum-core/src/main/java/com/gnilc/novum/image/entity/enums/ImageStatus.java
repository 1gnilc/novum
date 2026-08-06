package com.gnilc.novum.image.entity.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ImageStatus {
    PENDING("PENDING"),
    READY("READY");

    @EnumValue
    private final String value;

    ImageStatus(String value) {
        this.value = value;
    }

}
