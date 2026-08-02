package com.gnilc.novum.authz;

import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.novum.customer.support.CustomerApiTestConfiguration;
import com.gnilc.novum.support.SystemContainerContextInitializer;
import com.gnilc.novum.support.SystemTestApplication;
import com.gnilc.test.cleanup.BaselineDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Import(CustomerApiTestConfiguration.class)
@Transactional
class RequiredRoleAssignmentIT {
    @Autowired private BaselineDataSeeder baseline;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRoleService userRoles;

    @BeforeEach
    void restoreBaseline() {
        baseline.seed();
    }

    @Test
    void roleAssignmentCannotRemoveAdminOrCustomerBaselineRoles() {
        Long adminUserId = jdbc.queryForObject(
                "select user_id from sys_admin where username = 'admin' and del = 0", Long.class);
        Long customerUserId = jdbc.queryForObject(
                "select user_id from nv_customer where username = 'customer' and del = 0", Long.class);
        Long adminRoleId = roleId("admin");
        Long customerRoleId = roleId("customer");

        assertThatThrownBy(() -> userRoles.unbindRole(adminUserId, adminRoleId))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("The required baseline role cannot be removed.");
        assertThatThrownBy(() -> userRoles.unbindRole(customerUserId, customerRoleId))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("The required baseline role cannot be removed.");

        UserRoleDto assignment = new UserRoleDto();
        assignment.setUserId(customerUserId);
        assignment.setRoleIds(List.of());
        assertThatThrownBy(() -> userRoles.updateUserRole(assignment))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("The required baseline role cannot be removed.");
    }

    private Long roleId(String code) {
        return jdbc.queryForObject(
                "select id from az_role where code = ? and del = 0", Long.class, code);
    }
}
