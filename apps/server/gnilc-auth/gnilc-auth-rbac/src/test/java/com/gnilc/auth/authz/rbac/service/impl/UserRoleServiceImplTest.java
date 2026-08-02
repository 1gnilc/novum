package com.gnilc.auth.authz.rbac.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.gnilc.auth.authz.rbac.dao.UserRoleDao;
import com.gnilc.auth.authz.rbac.entity.bo.UserRoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.UserRoleDto;
import com.gnilc.auth.authz.rbac.service.RequiredRolePolicy;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UserRoleServiceImplTest extends RbacMessageTestSupport {
    private final UserRoleDao dao = mock(UserRoleDao.class);
    private final RequiredRolePolicy requiredRoles = (userId, roleId) -> userId == 7L && roleId == 11L;
    private UserRoleServiceImpl userRoles;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(UserRoleBo.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "user-role-service-test"),
                    UserRoleBo.class);
        }
        userRoles = org.mockito.Mockito.spy(new UserRoleServiceImpl(
                mock(ApplicationEventPublisher.class),
                List.of(requiredRoles),
                messages()));
        ReflectionTestUtils.setField(userRoles, "baseMapper", dao);
    }

    @Test
    void updateUserRoleRejectsMissingAssignmentWithTheDefaultLocale() {
        assertThatThrownBy(() -> userRoles.updateUserRole(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("User role assignment information is required.");
    }

    @Test
    void updateUserRoleRejectsRemovingARequiredRoleBeforeWriting() {
        doReturn(List.of(11L, 12L)).when(userRoles).getRoleIds(7L);
        UserRoleDto dto = new UserRoleDto();
        dto.setUserId(7L);
        dto.setRoleIds(List.of(12L));

        assertThatThrownBy(() -> userRoles.updateUserRole(dto))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("The required baseline role cannot be removed.");

        verify(dao, never()).delete(any());
        verify(dao, never()).insert(any(UserRoleBo.class));
    }

    @Test
    void unbindRoleRejectsARequiredRoleButAllowsOtherRoles() {
        assertThatThrownBy(() -> userRoles.unbindRole(7L, 11L))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("The required baseline role cannot be removed.");
        verify(dao, never()).delete(any());

        userRoles.unbindRole(7L, 12L);

        verify(dao).delete(any());
    }
}
