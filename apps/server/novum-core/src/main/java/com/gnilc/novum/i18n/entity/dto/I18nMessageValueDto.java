package com.gnilc.novum.i18n.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 单个语言的国际化消息值。
 */
@Data
public class I18nMessageValueDto {

    @NotBlank(message = "{system.i18n.locale.required}")
    private String locale;

    private String value;
}
