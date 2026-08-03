package com.gnilc.novum.authz;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gnilc.auth.authz.rbac.dao.RoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.service.RequiredRolePolicy;
import com.gnilc.novum.admin.dao.AdminDao;
import com.gnilc.novum.admin.entity.bo.AdminBo;
import com.gnilc.novum.customer.dao.CustomerDao;
import com.gnilc.novum.customer.entity.bo.CustomerBo;
import org.springframework.stereotype.Component;

/**
 * 声明 Novum 身份必须保留的基础角色。
 */
@Component
public class NovumRequiredRolePolicy implements RequiredRolePolicy {
    private static final String ADMIN_ROLE = "admin";
    private static final String CUSTOMER_ROLE = "customer";

    private final AdminDao adminDao;
    private final CustomerDao customerDao;
    private final RoleDao roleDao;

    public NovumRequiredRolePolicy(AdminDao adminDao, CustomerDao customerDao, RoleDao roleDao) {
        this.adminDao = adminDao;
        this.customerDao = customerDao;
        this.roleDao = roleDao;
    }

    @Override
    public boolean isRequired(Long userId, Long roleId) {
        if (userId == null || roleId == null) {
            return false;
        }
        RoleBo role = roleDao.selectById(roleId);
        if (role == null) {
            return false;
        }
        return switch (role.getCode()) {
            case ADMIN_ROLE -> hasAdmin(userId);
            case CUSTOMER_ROLE -> hasCustomer(userId);
            default -> false;
        };
    }

    private boolean hasAdmin(Long userId) {
        return adminDao.selectCount(Wrappers.<AdminBo>lambdaQuery()
                .eq(AdminBo::getUserId, userId)) > 0;
    }

    private boolean hasCustomer(Long userId) {
        return customerDao.selectCount(Wrappers.<CustomerBo>lambdaQuery()
                .eq(CustomerBo::getUserId, userId)) > 0;
    }
}
