package com.gnilc.novum.i18n.entity.dto;

import com.gnilc.common.utils.PageParams;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 国际化消息分页查询。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class I18nMessagePageDto extends PageParams {
    private String key;
    private String value;
    private String category;
    private String locale;
}
