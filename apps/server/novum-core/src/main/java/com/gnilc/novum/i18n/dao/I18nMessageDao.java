package com.gnilc.novum.i18n.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.novum.i18n.entity.bo.I18nMessageBo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 动态国际化消息 Mapper。
 */
@Mapper
public interface I18nMessageDao extends BaseMapper<I18nMessageBo> {
}
