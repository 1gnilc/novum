package com.gnilc.novum.i18n.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 国际化消息分页项。
 */
@Data
@AllArgsConstructor
public class I18nMessageItemVo {
    private String category;
    private String messageKey;
    private List<I18nMessageValueVo> values;
}
