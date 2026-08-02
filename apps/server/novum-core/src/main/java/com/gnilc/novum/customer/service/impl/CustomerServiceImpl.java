package com.gnilc.novum.customer.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.novum.auth.AccessPrincipalUtils;
import com.gnilc.novum.customer.dao.CustomerDao;
import com.gnilc.novum.customer.entity.bo.CustomerBo;
import com.gnilc.novum.customer.entity.vo.CustomerTokenVo;
import com.gnilc.novum.customer.entity.vo.CustomerVo;
import com.gnilc.novum.customer.service.CustomerService;
import com.gnilc.novum.session.CustomerSessionManager;
import com.gnilc.novum.session.SessionTokenPair;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 编排 Customer 资料和会话。
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerDao, CustomerBo>
        implements CustomerService {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final CustomerSessionManager sessionManager;
    private final RoleService roleService;

    public CustomerServiceImpl(CustomerSessionManager sessionManager, RoleService roleService) {
        this.sessionManager = sessionManager;
        this.roleService = roleService;
    }

    @Override
    public CustomerTokenVo login(String username, String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            return null;
        }
        CustomerBo customer = getCustomerByUsername(username);
        if (customer == null || Boolean.FALSE.equals(customer.getStatus())
                || !PASSWORD_ENCODER.matches(password, customer.getPassword())) {
            return null;
        }
        SessionTokenPair pair = sessionManager.createSession(customer.getUserId());
        return CustomerTokenVo.of(pair.getAccessToken(), pair.getRefreshToken());
    }

    @Override
    public CustomerTokenVo refresh(String refreshToken) {
        if (StringUtils.isBlank(refreshToken)) {
            return null;
        }
        SessionTokenPair pair = sessionManager.refreshSession(refreshToken);
        return pair == null ? null : CustomerTokenVo.of(pair.getAccessToken(), pair.getRefreshToken());
    }

    @Override
    public boolean logout(String refreshToken) {
        return StringUtils.isNotBlank(refreshToken) && sessionManager.logout(refreshToken);
    }

    @Override
    public CustomerVo getUserInfo() {
        Long userId = AccessPrincipalUtils.getUserId();
        CustomerBo customer = getCustomerByUserId(userId);
        if (customer == null) {
            return null;
        }
        CustomerVo info = new CustomerVo();
        BeanUtils.copyProperties(customer, info);
        info.setRoleCodes(getRoleCodes(userId));
        return info;
    }

    @Override
    public CustomerBo getCustomerByUsername(String username) {
        if (StringUtils.isBlank(username)) {
            return null;
        }
        return lambdaQuery().eq(CustomerBo::getUsername, username).one();
    }

    @Override
    public CustomerBo getCustomerByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return lambdaQuery().eq(CustomerBo::getUserId, userId).one();
    }

    private List<String> getRoleCodes(Long userId) {
        return Optional.ofNullable(roleService.getRoles(userId)).orElse(List.of()).stream()
                .map(RoleBo::getCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }
}
