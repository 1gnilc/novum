package com.gnilc.novum.authz;

import com.gnilc.auth.authz.rbac.dao.RoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.novum.admin.dao.AdminDao;
import com.gnilc.novum.customer.dao.CustomerDao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NovumRequiredRolePolicyTest {
    private final AdminDao admins = mock(AdminDao.class);
    private final CustomerDao customers = mock(CustomerDao.class);
    private final RoleDao roles = mock(RoleDao.class);
    private final NovumRequiredRolePolicy policy =
            new NovumRequiredRolePolicy(admins, customers, roles);

    @Test
    void adminAndCustomerBaselineRolesAreRequiredForTheirOwnIdentities() {
        when(roles.selectById(11L)).thenReturn(role("admin"));
        when(roles.selectById(12L)).thenReturn(role("customer"));
        when(admins.selectCount(any())).thenReturn(1L);
        when(customers.selectCount(any())).thenReturn(1L);

        assertThat(policy.isRequired(7L, 11L)).isTrue();
        assertThat(policy.isRequired(8L, 12L)).isTrue();
    }

    @Test
    void otherRolesAndUnrelatedIdentitiesAreNotRequired() {
        when(roles.selectById(11L)).thenReturn(role("admin"));
        when(roles.selectById(12L)).thenReturn(role("customer"));
        when(roles.selectById(13L)).thenReturn(role("manager"));

        assertThat(policy.isRequired(7L, 11L)).isFalse();
        assertThat(policy.isRequired(8L, 12L)).isFalse();
        assertThat(policy.isRequired(7L, 13L)).isFalse();
        assertThat(policy.isRequired(7L, 99L)).isFalse();
    }

    private RoleBo role(String code) {
        RoleBo role = new RoleBo();
        role.setCode(code);
        return role;
    }
}
