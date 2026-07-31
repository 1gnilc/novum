package com.gnilc.novum.i18n.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.common.utils.PageResult;
import com.gnilc.novum.i18n.entity.bo.I18nMessageBo;
import com.gnilc.novum.i18n.entity.dto.I18nMessagePageDto;
import com.gnilc.novum.i18n.entity.dto.I18nMessageDto;
import com.gnilc.novum.i18n.entity.vo.I18nMessageVo;
import com.gnilc.novum.i18n.entity.vo.I18nMessageItemVo;

import java.util.Map;
import java.util.List;

public interface DynamicI18nMessageService extends IService<I18nMessageBo> {

    Map<String, Object> getMessageBundle(String category);

    List<String> getSupportedCategories();

    PageResult<I18nMessageItemVo> getMessagePage(I18nMessagePageDto dto);

    I18nMessageVo getMessageValues(String messageKey);

    I18nMessageVo createMessage(I18nMessageDto dto);

    I18nMessageVo saveMessage(I18nMessageDto dto);

    void removeMessage(String messageKey);
}
