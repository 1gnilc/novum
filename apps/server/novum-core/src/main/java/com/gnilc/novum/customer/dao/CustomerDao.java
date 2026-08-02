package com.gnilc.novum.customer.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gnilc.novum.customer.entity.bo.CustomerBo;
import org.apache.ibatis.annotations.Mapper;

/**
 * Customer Mapper。
 */
@Mapper
public interface CustomerDao extends BaseMapper<CustomerBo> {
}
