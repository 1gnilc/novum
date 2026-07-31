package com.gnilc.novum.i18n.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 单个语言的国际化消息值。
 */
@Data
@AllArgsConstructor
public class I18nMessageValueVo {
    private String locale;
    private String value;
}
