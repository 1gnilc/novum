package com.gnilc.auth.authz.rbac.service.impl;

import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.event.AuthorizationEvent;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.common.exception.InvalidArgumentException;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.ApplicationEventPublisher;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RoleServiceImplTest extends RbacMessageTestSupport {
    @Test
    void createRoleRejectsMissingInformationWithTheDefaultLocale() {
        RoleServiceImpl roles = new RoleServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                mock(RolePermissionService.class),
                mock(RoleMenuService.class),
                messages());

        assertThatThrownBy(() -> roles.createRole(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Role information is required.");
    }

    @Test
    void createRoleRejectsWhitespaceOnlyNames() {
        RoleFixture fixture = roleFixture();
        RoleDto dto = new RoleDto();
        dto.setCode("operator");
        dto.setName("   ");

        assertThatThrownBy(() -> fixture.getService().createRole(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Role name is required.");
        verifyNoRoleWrite(fixture);
    }

    @Test
    void createRoleRejectsWhitespaceOnlyCodes() {
        RoleFixture fixture = roleFixture();
        RoleDto dto = validRole();
        dto.setCode("   ");

        assertThatThrownBy(() -> fixture.getService().createRole(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Role code is required.");
        verifyNoRoleWrite(fixture);
    }

    @ParameterizedTest(name = "accepts exact {0} business limit")
    @MethodSource("roleLengthBoundaries")
    void createRoleAcceptsExactBusinessLimits(
            String field,
            int maximum,
            String character,
            String ignoredMessage) {
        RoleFixture fixture = roleFixture();
        RoleDto dto = validRole();
        setField(dto, field, character.repeat(maximum));
        doReturn(null).when(fixture.getService()).getRoleByCode(dto.getCode());

        fixture.getService().createRole(dto);

        verify(fixture.getService()).save(any(RoleBo.class));
        verify(fixture.getPublisher()).publishEvent(any(AuthorizationEvent.class));
    }

    @ParameterizedTest(name = "rejects {0} beyond business limit")
    @MethodSource("roleLengthBoundaries")
    void createRoleRejectsFieldsBeyondBusinessLimits(
            String field,
            int maximum,
            String character,
            String message) {
        RoleFixture fixture = roleFixture();
        RoleDto dto = validRole();
        setField(dto, field, character.repeat(maximum + 1));

        assertThatThrownBy(() -> fixture.getService().createRole(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage(message);
        verifyNoRoleWrite(fixture);
    }

    @Test
    void removeRoleClearsAllRelationships() {
        UserRoleService userRoles = mock(UserRoleService.class);
        RolePermissionService rolePermissions = mock(RolePermissionService.class);
        RoleMenuService roleMenus = mock(RoleMenuService.class);
        RoleServiceImpl roles = spy(new RoleServiceImpl(
                mock(ApplicationEventPublisher.class),
                userRoles,
                rolePermissions,
                roleMenus,
                messages()));
        RoleBo role = new RoleBo();
        role.setId(7L);
        String originalCode = "\uD83D\uDE00".repeat(255);
        role.setCode(originalCode);
        role.setBuiltIn(false);
        doReturn(role).when(roles).getById(7L);
        doReturn(true).when(roles).updateById(role);
        doReturn(true).when(roles).removeById(7L);

        roles.removeRole(7L);

        verify(rolePermissions).removeByRoleId(7L);
        verify(roleMenus).removeByRoleId(7L);
        verify(userRoles).removeByRoleId(7L);
        verify(roles).removeById(7L);
        assertThat(role.getCode()).isEqualTo(originalCode + "_del_7");
    }

    private RoleFixture roleFixture() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        RoleServiceImpl service = spy(new RoleServiceImpl(
                publisher,
                mock(UserRoleService.class),
                mock(RolePermissionService.class),
                mock(RoleMenuService.class),
                messages()));
        doAnswer(invocation -> {
            ((RoleBo) invocation.getArgument(0)).setId(10L);
            return true;
        }).when(service).save(any(RoleBo.class));
        return new RoleFixture(service, publisher);
    }

    private RoleDto validRole() {
        RoleDto dto = new RoleDto();
        dto.setCode("operator");
        dto.setName("Operator");
        dto.setRemark("Operates the system");
        return dto;
    }

    private void verifyNoRoleWrite(RoleFixture fixture) {
        verify(fixture.getService(), never()).save(any(RoleBo.class));
        verify(fixture.getService(), never()).updateById(any(RoleBo.class));
        verifyNoInteractions(fixture.getPublisher());
    }

    private static void setField(RoleDto dto, String field, String value) {
        switch (field) {
            case "code" -> dto.setCode(value);
            case "name" -> dto.setName(value);
            case "remark" -> dto.setRemark(value);
            default -> throw new IllegalArgumentException("Unknown role field: " + field);
        }
    }

    private static Stream<Arguments> roleLengthBoundaries() {
        return Stream.of(
                Arguments.of("code", 255, "r", "Role code must not exceed 255 characters."),
                Arguments.of("name", 255, "\uD83D\uDE00", "Role name must not exceed 255 characters."),
                Arguments.of("remark", 500, "m", "Role description must not exceed 500 characters."));
    }

    @Data
    private static final class RoleFixture {
        private final RoleServiceImpl service;
        private final ApplicationEventPublisher publisher;
    }
}
