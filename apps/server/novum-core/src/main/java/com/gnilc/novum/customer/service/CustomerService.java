package com.gnilc.novum.customer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gnilc.novum.customer.entity.bo.CustomerBo;
import com.gnilc.novum.customer.entity.vo.CustomerTokenVo;
import com.gnilc.novum.customer.entity.vo.CustomerVo;

/**
 * Customer 应用服务。
 */
public interface CustomerService extends IService<CustomerBo> {
    CustomerTokenVo login(String username, String password);

    CustomerTokenVo refresh(String refreshToken);

    boolean logout(String refreshToken);

    CustomerVo getUserInfo();

    CustomerBo getCustomerByUsername(String username);

    CustomerBo getCustomerByUserId(Long userId);
}
