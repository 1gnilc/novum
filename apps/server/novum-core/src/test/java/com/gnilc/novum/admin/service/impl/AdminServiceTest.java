package com.gnilc.novum.admin.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authz.rbac.entity.vo.MenuRouteVo;
import com.gnilc.auth.authz.rbac.service.MenuService;
import com.gnilc.auth.authz.rbac.service.RoleMenuService;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.auth.authz.rbac.service.UserRoleService;
import com.gnilc.auth.authz.rbac.service.UserService;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.exception.AuthenticationFailedException;
import com.gnilc.common.exception.UnauthorizedException;
import com.gnilc.common.i18n.I18nMessageService;
import com.gnilc.novum.admin.cache.AdminCacheService;
import com.gnilc.novum.admin.dao.AdminDao;
import com.gnilc.novum.admin.entity.bo.AdminBo;
import com.gnilc.novum.admin.entity.dto.AdminDto;
import com.gnilc.novum.admin.entity.vo.AdminVo;
import com.gnilc.novum.admin.event.AdminEvent;
import com.gnilc.novum.session.AdminSessionManager;
import com.gnilc.novum.session.SessionTokenPair;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    private static final long ADMIN_ID = 41L;
    private static final long USER_ID = 84L;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Mock
    private AdminDao adminDao;
    @Mock
    private AdminSessionManager sessions;
    @Mock
    private AdminCacheService cacheService;
    @Mock
    private RoleService roles;
    @Mock
    private MenuService menus;
    @Mock
    private RoleMenuService roleMenus;
    @Mock
    private UserService users;
    @Mock
    private UserRoleService userRoles;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AdminServiceImpl admins;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        if (TableInfoHelper.getTableInfo(AdminBo.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "admin-service-test"),
                    AdminBo.class);
        }
        LocaleContextHolder.setLocale(Locale.US);
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasenames("i18n/rbac/messages", "i18n/system/messages");
        source.setDefaultEncoding("UTF-8");
        I18nMessageService messages = new I18nMessageService(source, "en-US");
        admins = spy(new AdminServiceImpl(
                sessions,
                cacheService,
                roles,
                menus,
                roleMenus,
                users,
                userRoles,
                eventPublisher,
                messages));
        lenient().when(cacheService.getUserInfo(any(), any()))
                .thenAnswer(invocation -> ((Supplier<AdminVo>) invocation.getArgument(1)).get());
        lenient().when(cacheService.getRoleCodes(any(), any()))
                .thenAnswer(invocation -> ((Supplier<List<String>>) invocation.getArgument(1)).get());
        lenient().when(cacheService.getMenuAccessCodes(any(), any()))
                .thenAnswer(invocation -> ((Supplier<List<String>>) invocation.getArgument(1)).get());
        lenient().when(cacheService.getMenuRoutes(any(), any()))
                .thenAnswer(invocation -> ((Supplier<List<MenuRouteVo>>) invocation.getArgument(1)).get());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of(USER_ID));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void loginCreatesAnAdminSessionForValidCredentials() {
        AdminBo admin = currentAdmin();
        doReturn(admin).when(admins).getAdminByUsername("admin");
        when(sessions.createSession(USER_ID)).thenReturn(SessionTokenPair.of("access", "refresh"));

        var token = admins.login("admin", "Initial#123");

        assertThat(token.getAccessToken()).isEqualTo("access");
        assertThat(token.getRefreshToken()).isEqualTo("refresh");
        verify(sessions).createSession(USER_ID);
    }

    @Test
    void loginRejectsInvalidCredentialsAndDisabledAdmins() {
        assertThatThrownBy(() -> admins.login(null, "Initial#123"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Incorrect username or password.");
        assertThatThrownBy(() -> admins.login("admin", " "))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Incorrect username or password.");

        AdminBo admin = currentAdmin();
        admin.setStatus(false);
        doReturn(admin).when(admins).getAdminByUsername("admin");
        assertThatThrownBy(() -> admins.login("admin", "Initial#123"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Incorrect username or password.");

        admin.setStatus(true);
        assertThatThrownBy(() -> admins.login("admin", "wrong"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Incorrect username or password.");
        verify(sessions, never()).createSession(any());
    }

    @Test
    void refreshAndLogoutDelegateToTheAdminSessionManager() {
        when(sessions.refreshSession("refresh"))
                .thenReturn(SessionTokenPair.of("new-access", "refresh"));
        when(sessions.logout("refresh")).thenReturn(true);

        var token = admins.refresh("refresh");

        assertThat(token.getAccessToken()).isEqualTo("new-access");
        assertThat(token.getRefreshToken()).isEqualTo("refresh");
        admins.logout("refresh");
        verify(sessions).logout("refresh");
    }

    @Test
    void refreshRejectsMissingAndInvalidRefreshTokens() {
        when(sessions.refreshSession("invalid")).thenReturn(null);

        assertThatThrownBy(() -> admins.refresh(" "))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Your login has expired. Please sign in again.");
        assertThatThrownBy(() -> admins.refresh("invalid"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Your login has expired. Please sign in again.");
    }

    @Test
    void logoutRejectsMissingAndInvalidRefreshTokens() {
        when(sessions.logout("invalid")).thenReturn(false);

        assertThatThrownBy(() -> admins.logout(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Unauthorized.");
        assertThatThrownBy(() -> admins.logout("invalid"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Unauthorized.");
    }

    @Test
    void updateProfileUsesCurrentUserAndOnlyWritesEditableFields() {
        stubQueryChain();
        stubUpdateChain();
        when(adminDao.selectOne(any())).thenReturn(currentAdmin());
        when(adminDao.update(isNull(), any())).thenReturn(1);
        AdminDto profile = new AdminDto();
        profile.setId(999L);
        profile.setUsername("other");
        profile.setNickname("  Updated Admin  ");
        profile.setAvatar("  ");
        profile.setDesc(" ");
        profile.setHomePath("/other");
        profile.setStatus(false);

        admins.updateProfile(profile);

        ArgumentCaptor<Wrapper<AdminBo>> update = wrapperCaptor();
        verify(adminDao).update(isNull(), update.capture());
        Wrapper<AdminBo> wrapper = update.getValue();
        LambdaUpdateWrapper<AdminBo> lambdaUpdate = asLambdaUpdate(wrapper);
        assertThat(wrapper.getSqlSet())
                .contains("nickname", "avatar", "description")
                .doesNotContain("username", "home_path", "status");
        assertThat(wrapper.getSqlSegment()).contains("id");
        assertThat(lambdaUpdate.getParamNameValuePairs())
                .containsValue("Updated Admin")
                .containsValue(ADMIN_ID);
        verify(eventPublisher).publishEvent(new AdminEvent(AdminEvent.Action.UPDATE, USER_ID));
    }

    @Test
    void updateProfileRejectsOversizedProfileFieldsBeforeWriting() {
        AdminDto profile = new AdminDto();
        profile.setNickname("n".repeat(256));

        assertThatThrownBy(() -> admins.updateProfile(profile))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Nickname must be at most 255 characters.");

        profile.setNickname("Admin");
        profile.setAvatar("a".repeat(501));
        assertThatThrownBy(() -> admins.updateProfile(profile))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Avatar URL must be at most 500 characters.");

        profile.setAvatar(null);
        profile.setDesc("d".repeat(501));
        assertThatThrownBy(() -> admins.updateProfile(profile))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Description must be at most 500 characters.");
        verify(adminDao, never()).update(isNull(), any());
    }

    @Test
    void updateProfileUsesTheRequestLocaleForValidationErrors() {
        LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
        AdminDto profile = new AdminDto();
        profile.setNickname("n".repeat(256));

        assertThatThrownBy(() -> admins.updateProfile(profile))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("昵称长度不能超过 255 个字符。");
    }

    @Test
    void updatePasswordEncodesPasswordAndRevokesAllCurrentUserSessions() {
        stubQueryChain();
        stubUpdateChain();
        when(adminDao.selectOne(any())).thenReturn(currentAdmin());
        when(adminDao.update(isNull(), any())).thenReturn(1);

        admins.updatePassword("Initial#123", "Changed#456");

        ArgumentCaptor<Wrapper<AdminBo>> update = wrapperCaptor();
        verify(adminDao).update(isNull(), update.capture());
        assertThat(asLambdaUpdate(update.getValue()).getParamNameValuePairs().values())
                .anyMatch(value -> value instanceof String passwordHash
                        && PASSWORD_ENCODER.matches("Changed#456", passwordHash));
        verify(sessions).cleanupUserSessions(USER_ID);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updatePasswordRejectsWrongCurrentPasswordWithoutWritingOrRevokingSessions() {
        stubQueryChain();
        when(adminDao.selectOne(any())).thenReturn(currentAdmin());

        assertThatThrownBy(() -> admins.updatePassword("Wrong#123", "Changed#456"))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Current password is incorrect.");
        verify(adminDao, never()).update(isNull(), any());
        verify(sessions, never()).cleanupUserSessions(any());
    }

    @Test
    void getMenuRoutesUsesTheCurrentUserRoleMenuBindings() {
        MenuRouteVo route = new MenuRouteVo();
        route.setName("Dashboard");
        when(userRoles.getRoleIds(USER_ID)).thenReturn(List.of(21L));
        when(roleMenus.getMenuIds(List.of(21L))).thenReturn(List.of(31L));
        when(menus.getMenuRoutes(List.of(31L))).thenReturn(List.of(route));

        assertThat(admins.getMenuRoutes()).containsExactly(route);
        verify(cacheService).getMenuRoutes(any(), any());
        verify(userRoles).getRoleIds(USER_ID);
        verify(roleMenus).getMenuIds(List.of(21L));
        verify(menus).getMenuRoutes(List.of(31L));
    }

    @Test
    void updateAdminRejectsDisablingTheCurrentAdministrator() {
        AdminBo current = currentAdmin();
        doReturn(current).when(admins).getAdmin(ADMIN_ID);
        AdminDto update = new AdminDto();
        update.setId(ADMIN_ID);
        update.setUsername("admin");
        update.setNickname("Administrator");
        update.setStatus(false);

        assertThatThrownBy(() -> admins.updateAdmin(update))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("The current administrator cannot disable itself.");
        verify(admins, never()).updateById(any());
    }

    @ParameterizedTest(name = "status {0} -> {1}, revoke sessions: {2}")
    @MethodSource("statusTransitions")
    void updateAdminRevokesSessionsOnlyWhenAnEnabledAdministratorIsDisabled(
            Boolean currentStatus,
            Boolean submittedStatus,
            boolean shouldRevokeSessions) {
        AdminBo target = currentAdmin();
        target.setUserId(USER_ID + 1);
        target.setStatus(currentStatus);
        doReturn(target).when(admins).getAdmin(ADMIN_ID);
        doReturn(true).when(admins).updateById(any());
        AdminDto update = new AdminDto();
        update.setId(ADMIN_ID);
        update.setStatus(submittedStatus);

        admins.updateAdmin(update);

        if (shouldRevokeSessions) {
            verify(sessions).cleanupUserSessions(USER_ID + 1);
        } else {
            verify(sessions, never()).cleanupUserSessions(any());
        }
        verify(eventPublisher).publishEvent(new AdminEvent(AdminEvent.Action.UPDATE, USER_ID + 1));
    }

    @Test
    void createAdminRejectsWhitespaceOnlyNicknames() {
        AdminDto create = new AdminDto();
        create.setUsername("operator");
        create.setPassword("Strong#123");
        create.setNickname("   ");

        assertThatThrownBy(() -> admins.createAdmin(create))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessage("Nickname is required.");
        verify(users, never()).createUser();
    }

    @Test
    void removeAdminRejectsDeletingTheCurrentAdministrator() {
        AdminBo current = currentAdmin();
        doReturn(current).when(admins).getById(ADMIN_ID);

        assertThatThrownBy(() -> admins.removeAdmin(ADMIN_ID))
                .isInstanceOf(IllegalConditionException.class)
                .hasMessage("The current administrator cannot delete itself.");
        verify(sessions, never()).cleanupUserSessions(any());
        verify(admins, never()).removeById(ADMIN_ID);
    }

    @Test
    void removeAdminPreservesUnicodeBoundariesInTheReleasedUsername() {
        AdminBo target = currentAdmin();
        target.setUserId(USER_ID + 1);
        String originalUsername = "\uD83D\uDE00".repeat(255);
        target.setUsername(originalUsername);
        doReturn(target).when(admins).getById(ADMIN_ID);
        doReturn(true).when(admins).updateById(target);
        doReturn(true).when(admins).removeById(ADMIN_ID);

        admins.removeAdmin(ADMIN_ID);

        assertThat(target.getUsername()).isEqualTo(originalUsername + "_del_41");
        verify(admins).updateById(target);
        verify(admins).removeById(ADMIN_ID);
    }

    private static Stream<Arguments> statusTransitions() {
        return Stream.of(
                Arguments.of(true, false, true),
                Arguments.of(true, true, false),
                Arguments.of(true, null, false),
                Arguments.of(false, false, false),
                Arguments.of(false, true, false),
                Arguments.of(false, null, false),
                Arguments.of(null, false, false),
                Arguments.of(null, true, false),
                Arguments.of(null, null, false));
    }

    private void stubQueryChain() {
        doAnswer(invocation -> new LambdaQueryChainWrapper<>(
                adminDao, Wrappers.lambdaQuery(AdminBo.class)))
                .when(admins).lambdaQuery();
    }

    private void stubUpdateChain() {
        doAnswer(invocation -> new LambdaUpdateChainWrapper<>(
                adminDao, Wrappers.lambdaUpdate(AdminBo.class)))
                .when(admins).lambdaUpdate();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<Wrapper<AdminBo>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<AdminBo> asLambdaUpdate(Wrapper<AdminBo> wrapper) {
        assertThat(wrapper).isInstanceOf(LambdaUpdateWrapper.class);
        return (LambdaUpdateWrapper<AdminBo>) wrapper;
    }

    private AdminBo currentAdmin() {
        AdminBo admin = new AdminBo();
        admin.setId(ADMIN_ID);
        admin.setUserId(USER_ID);
        admin.setUsername("admin");
        admin.setPassword(PASSWORD_ENCODER.encode("Initial#123"));
        admin.setStatus(true);
        return admin;
    }
}
