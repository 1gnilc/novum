package com.gnilc.auth.authz.rbac.service.impl;

import com.gnilc.auth.authz.rbac.entity.bo.PermissionBo;
import com.gnilc.auth.authz.rbac.entity.dto.PermissionDto;
import com.gnilc.auth.authz.rbac.event.AuthorizationEvent;
import com.gnilc.auth.authz.rbac.service.RolePermissionService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.common.exception.IllegalConditionException;
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

class PermissionServiceImplTest extends RbacMessageTestSupport {
    @Test
    void createPermissionRejectsMissingInformationWithTheDefaultLocale() {
        PermissionServiceImpl permissions = new PermissionServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                mock(RolePermissionService.class),
                messages());

        assertThatThrownBy(() -> permissions.createPermission(null))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Permission information is required.");
    }

    @Test
    void updateAndRemoveRejectBuiltInPermissions() {
        PermissionServiceImpl permissions = spy(new PermissionServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                mock(RolePermissionService.class),
                messages()));
        PermissionBo builtIn = new PermissionBo();
        builtIn.setId(1L);
        builtIn.setBuiltIn(true);
        doReturn(builtIn).when(permissions).getById(1L);
        PermissionDto update = new PermissionDto();
        update.setId(1L);

        assertThatThrownBy(() -> permissions.updatePermission(update))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("Built-in permissions cannot be modified.");
        assertThatThrownBy(() -> permissions.removePermission(1L))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("Built-in permissions cannot be deleted.");
    }

    @ParameterizedTest(name = "rejects blank required field {0}")
    @MethodSource("permissionRequiredFields")
    void createPermissionRejectsWhitespaceOnlyRequiredFields(String field, String message) {
        PermissionFixture fixture = permissionFixture();
        PermissionDto dto = validPermission();
        setField(dto, field, "   ");

        assertThatThrownBy(() -> fixture.getService().createPermission(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage(message);
        verifyNoPermissionWrite(fixture);
    }

    @ParameterizedTest(name = "accepts exact {0} business limit")
    @MethodSource("permissionLengthBoundaries")
    void createPermissionAcceptsExactBusinessLimits(
            String field,
            int maximum,
            String character,
            String ignoredMessage) {
        PermissionFixture fixture = permissionFixture();
        PermissionDto dto = validPermission();
        setField(dto, field, character.repeat(maximum));
        doReturn(null).when(fixture.getService()).getPermissionByCode(dto.getCode());

        fixture.getService().createPermission(dto);

        verify(fixture.getService()).save(any(PermissionBo.class));
        verify(fixture.getPublisher()).publishEvent(any(AuthorizationEvent.class));
    }

    @ParameterizedTest(name = "rejects {0} beyond business limit")
    @MethodSource("permissionLengthBoundaries")
    void createPermissionRejectsFieldsBeyondBusinessLimits(
            String field,
            int maximum,
            String character,
            String message) {
        PermissionFixture fixture = permissionFixture();
        PermissionDto dto = validPermission();
        setField(dto, field, character.repeat(maximum + 1));

        assertThatThrownBy(() -> fixture.getService().createPermission(dto))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage(message);
        verifyNoPermissionWrite(fixture);
    }

    @Test
    void removePermissionClearsRoleBindings() {
        RolePermissionService rolePermissions = mock(RolePermissionService.class);
        PermissionServiceImpl permissions = spy(new PermissionServiceImpl(
                mock(ApplicationEventPublisher.class),
                mock(UserRoleService.class),
                rolePermissions,
                messages()));
        PermissionBo permission = new PermissionBo();
        permission.setId(2L);
        String originalCode = "\uD83D\uDE00".repeat(255);
        permission.setCode(originalCode);
        permission.setBuiltIn(false);
        doReturn(permission).when(permissions).getById(2L);
        doReturn(true).when(permissions).updateById(permission);
        doReturn(true).when(permissions).removeById(2L);

        permissions.removePermission(2L);

        verify(rolePermissions).removeByPermissionId(2L);
        verify(permissions).removeById(2L);
        assertThat(permission.getCode()).isEqualTo(originalCode + "_del_2");
    }

    private PermissionFixture permissionFixture() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        PermissionServiceImpl service = spy(new PermissionServiceImpl(
                publisher,
                mock(UserRoleService.class),
                mock(RolePermissionService.class),
                messages()));
        doAnswer(invocation -> {
            ((PermissionBo) invocation.getArgument(0)).setId(20L);
            return true;
        }).when(service).save(any(PermissionBo.class));
        return new PermissionFixture(service, publisher);
    }

    private PermissionDto validPermission() {
        PermissionDto dto = new PermissionDto();
        dto.setCode("reports:read");
        dto.setName("Read reports");
        dto.setTargetIdentifier("/reports/**");
        dto.setTargetQualifier("GET");
        dto.setRemark("Allows reports to be read");
        dto.setPublicAccess(false);
        return dto;
    }

    private void verifyNoPermissionWrite(PermissionFixture fixture) {
        verify(fixture.getService(), never()).save(any(PermissionBo.class));
        verify(fixture.getService(), never()).updateById(any(PermissionBo.class));
        verifyNoInteractions(fixture.getPublisher());
    }

    private static void setField(PermissionDto dto, String field, String value) {
        switch (field) {
            case "code" -> dto.setCode(value);
            case "name" -> dto.setName(value);
            case "targetIdentifier" -> dto.setTargetIdentifier(value);
            case "targetQualifier" -> dto.setTargetQualifier(value);
            case "remark" -> dto.setRemark(value);
            default -> throw new IllegalArgumentException("Unknown permission field: " + field);
        }
    }

    private static Stream<Arguments> permissionRequiredFields() {
        return Stream.of(
                Arguments.of("code", "Permission code is required."),
                Arguments.of("name", "Permission name is required."),
                Arguments.of("targetIdentifier", "Access target identifier is required."));
    }

    private static Stream<Arguments> permissionLengthBoundaries() {
        return Stream.of(
                Arguments.of("code", 255, "p", "Permission code must not exceed 255 characters."),
                Arguments.of("name", 255, "\uD83D\uDE00", "Permission name must not exceed 255 characters."),
                Arguments.of("targetIdentifier", 500, "t",
                        "Access target identifier must not exceed 500 characters."),
                Arguments.of("targetQualifier", 100, "q", "Target qualifier must not exceed 100 characters."),
                Arguments.of("remark", 500, "m", "Permission description must not exceed 500 characters."));
    }

    @Data
    private static final class PermissionFixture {
        private final PermissionServiceImpl service;
        private final ApplicationEventPublisher publisher;
    }
}
