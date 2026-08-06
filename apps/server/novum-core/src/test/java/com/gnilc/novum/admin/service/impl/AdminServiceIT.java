package com.gnilc.novum.admin.service.impl;

import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.common.exception.AuthenticationFailedException;
import com.gnilc.common.exception.IllegalConditionException;
import com.gnilc.common.exception.InvalidArgumentException;
import com.gnilc.common.exception.UnauthorizedException;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.novum.admin.cache.AdminCacheService;
import com.gnilc.novum.admin.entity.bo.AdminBo;
import com.gnilc.novum.admin.entity.dto.AdminDto;
import com.gnilc.novum.admin.entity.dto.AdminPageDto;
import com.gnilc.novum.admin.entity.dto.AdminRoleDto;
import com.gnilc.novum.admin.entity.vo.AdminTokenVo;
import com.gnilc.novum.admin.service.AdminService;
import com.gnilc.novum.admin.entity.vo.AdminVo;
import com.gnilc.novum.session.AdminSessionManager;
import com.gnilc.novum.support.SystemTestApplication;
import com.gnilc.novum.support.SystemContainerContextInitializer;
import com.gnilc.test.cleanup.RedisCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
@Transactional
class AdminServiceIT {
    @Autowired private AdminService admins;
    @Autowired private AdminCacheService adminCache;
    @Autowired private RoleService roles;
    @Autowired private RedisConnectionFactory redis;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AdminSessionManager sessions;

    @BeforeEach
    void cleanRedisBeforeTest() {
        cleanRedis();
        ensureRole("admin");
    }

    @AfterEach
    void cleanRedisAfterTest() {
        RequestContextHolder.resetRequestAttributes();
        cleanRedis();
    }

    private void cleanRedis() {
        new RedisCleaner(redis).flushDatabase();
    }

    @Test
    void createLoginPageAndRemoveAdminRemainConsistentWithRbacUser() {
        ensureRole("operator");
        AdminDto create = admin("alice", "Strong#123", List.of("operator"));
        admins.createAdmin(create);
        AdminBo stored = admins.getAdminByUsername("alice");

        assertThat(stored.getUserId()).isNotNull();
        assertThat(new BCryptPasswordEncoder().matches("Strong#123", stored.getPassword())).isTrue();
        assertThat(admins.getRoleCodes(stored.getUserId()))
                .containsExactlyInAnyOrder("admin", "operator");

        AdminTokenVo token = admins.login("alice", "Strong#123");
        assertThat(token).isNotNull();
        assertThat(token.getAccessToken()).startsWith("sys_admin." + stored.getUserId() + ".");
        admins.createAdmin(admin("other-admin", "Strong#123", null));
        AdminTokenVo otherToken = admins.login("other-admin", "Strong#123");

        AdminPageDto query = new AdminPageDto();
        query.setUsername("alice");
        assertThat(admins.getAdminPage(query).getList())
                .extracting(com.gnilc.novum.admin.entity.vo.AdminVo::getUsername)
                .containsExactly("alice");

        authenticateAs(admins.getAdminByUsername("other-admin").getUserId());
        admins.removeAdmin(stored.getId());
        assertThat(admins.getAdmin(stored.getId())).isNull();
        assertThatThrownBy(() -> admins.login("alice", "Strong#123"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Incorrect username or password.");
        assertThat(sessions.validateAccessToken(token.getAccessToken())).isNull();
        assertThatThrownBy(() -> admins.refresh(token.getRefreshToken()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Your login has expired. Please sign in again.");
        assertThat(sessions.validateAccessToken(otherToken.getAccessToken()))
                .isEqualTo(admins.getAdminByUsername("other-admin").getUserId());
        assertThat(admins.refresh(otherToken.getRefreshToken())).isNotNull();
        assertThat(jdbc.queryForObject(
                "select username from sys_admin where id = ?", String.class, stored.getId()))
                .isEqualTo("alice_del_" + stored.getId());
        assertThat(jdbc.queryForObject(
                "select del from az_user where id = ?", Integer.class, stored.getUserId()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select count(*)
                  from az_user_role
                 where user_id = ? and del = 0
                """, Integer.class, stored.getUserId())).isZero();

        admins.createAdmin(admin("alice", "Replacement#123", List.of("operator")));
        assertThat(admins.getAdminByUsername("alice").getId()).isNotEqualTo(stored.getId());
    }

    @Test
    void updateCanReplaceRolesChangePasswordAndDisableSessions() {
        ensureRole("reviewer");
        admins.createAdmin(admin("bob", "Initial#123", List.of()));
        AdminBo bob = admins.getAdminByUsername("bob");
        AdminTokenVo firstBobSession = admins.login("bob", "Initial#123");
        AdminTokenVo secondBobSession = admins.login("bob", "Initial#123");
        admins.createAdmin(admin("still-enabled", "Initial#123", List.of()));
        AdminTokenVo enabledSession = admins.login("still-enabled", "Initial#123");

        AdminDto update = new AdminDto();
        update.setId(bob.getId());
        update.setPassword("Changed#456");
        update.setNickname("Robert");
        update.setStatus(false);
        update.setRoleCodes(List.of("reviewer"));
        authenticateAs(admins.getAdminByUsername("still-enabled").getUserId());
        admins.updateAdmin(update);

        assertThat(admins.getAdmin(bob.getId()).getNickname()).isEqualTo("Robert");
        assertThat(admins.getRoleCodes(bob.getUserId()))
                .containsExactlyInAnyOrder("admin", "reviewer");
        assertThatThrownBy(() -> admins.login("bob", "Changed#456"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Incorrect username or password.");
        assertThat(sessions.validateAccessToken(firstBobSession.getAccessToken())).isNull();
        assertThat(sessions.validateAccessToken(secondBobSession.getAccessToken())).isNull();
        assertThatThrownBy(() -> admins.refresh(firstBobSession.getRefreshToken()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Your login has expired. Please sign in again.");
        assertThatThrownBy(() -> admins.refresh(secondBobSession.getRefreshToken()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Your login has expired. Please sign in again.");
        assertThat(sessions.validateAccessToken(enabledSession.getAccessToken()))
                .isEqualTo(admins.getAdminByUsername("still-enabled").getUserId());
        assertThat(admins.refresh(enabledSession.getRefreshToken())).isNotNull();
    }

    @Test
    void updateKeepsPasswordWhenPasswordIsBlankOrNull() {
        admins.createAdmin(admin("password-kept", "Initial#123", List.of()));
        AdminBo stored = admins.getAdminByUsername("password-kept");

        AdminDto blankPassword = new AdminDto();
        blankPassword.setId(stored.getId());
        blankPassword.setPassword("  ");
        admins.updateAdmin(blankPassword);
        assertThat(admins.login("password-kept", "Initial#123")).isNotNull();

        AdminDto nullPassword = new AdminDto();
        nullPassword.setId(stored.getId());
        nullPassword.setPassword(null);
        admins.updateAdmin(nullPassword);
        assertThat(admins.login("password-kept", "Initial#123")).isNotNull();
    }

    @Test
    void updateClearsExplicitlySubmittedNullableProfileFields() {
        AdminDto create = admin("profile-clear", "Initial#123", List.of());
        create.setAvatar("images/2026/08/05/79f91166-e852-4a9d-a419-dabfb427cb8c.png");
        create.setDesc("Temporary description");
        admins.createAdmin(create);
        AdminBo stored = admins.getAdminByUsername("profile-clear");

        AdminDto omitted = new AdminDto();
        omitted.setId(stored.getId());
        omitted.setNickname("Profile unchanged");
        admins.updateAdmin(omitted);
        assertThat(admins.getAdmin(stored.getId())).satisfies(admin -> {
            assertThat(admin.getAvatar()).isEqualTo(
                    "images/2026/08/05/79f91166-e852-4a9d-a419-dabfb427cb8c.png");
            assertThat(admin.getDescription()).isEqualTo("Temporary description");
        });

        AdminDto update = new AdminDto();
        update.setId(stored.getId());
        update.setAvatar(null);
        update.setDesc(null);
        admins.updateAdmin(update);

        AdminBo updated = admins.getAdmin(stored.getId());
        assertThat(updated.getAvatar()).isNull();
        assertThat(updated.getDescription()).isNull();
    }

    @Test
    void roleReplacementSupportsEmptyListAndRejectsUnknownRole() {
        ensureRole("operator");
        admins.createAdmin(admin("carol", "Strong#123", List.of("operator")));
        AdminBo carol = admins.getAdminByUsername("carol");

        AdminDto profileOnly = new AdminDto();
        profileOnly.setId(carol.getId());
        profileOnly.setNickname("Carol Updated");
        admins.updateAdmin(profileOnly);
        assertThat(admins.getRoleCodes(carol.getUserId()))
                .containsExactlyInAnyOrder("admin", "operator");

        AdminRoleDto clear = new AdminRoleDto();
        clear.setId(carol.getId());
        clear.setRoleCodes(List.of());
        admins.saveAdminRoles(clear);
        // The class-level test transaction reaches BEFORE_COMMIT only after this assertion.
        adminCache.removeRoleCodes(carol.getUserId());
        assertThat(admins.getRoleCodes(carol.getUserId())).containsExactly("admin");

        clear.setRoleCodes(List.of("operator"));
        admins.saveAdminRoles(clear);
        adminCache.removeRoleCodes(carol.getUserId());
        assertThat(admins.getRoleCodes(carol.getUserId()))
                .containsExactlyInAnyOrder("admin", "operator");

        AdminDto clearThroughUpdate = new AdminDto();
        clearThroughUpdate.setId(carol.getId());
        clearThroughUpdate.setRoleCodes(List.of());
        admins.updateAdmin(clearThroughUpdate);
        adminCache.removeRoleCodes(carol.getUserId());
        assertThat(admins.getRoleCodes(carol.getUserId())).containsExactly("admin");

        clear.setRoleCodes(List.of("missing"));
        assertThatThrownBy(() -> admins.saveAdminRoles(clear))
                .isInstanceOf(IllegalConditionException.class);
    }

    @Test
    void createRejectsDuplicateUsernames() {
        admins.createAdmin(admin("unique", "Strong#123", null));
        assertThatThrownBy(() -> admins.createAdmin(admin("unique", "Another#123", null)))
                .isInstanceOf(InvalidArgumentException.class);
    }

    @ParameterizedTest(name = "accepts {0}")
    @MethodSource("validPasswordBoundaries")
    void createAcceptsPasswordLengthBoundaries(String username, String password) {
        admins.createAdmin(admin(username, password, null));

        assertThat(admins.login(username, password)).isNotNull();
    }

    @ParameterizedTest(name = "rejects {0}")
    @MethodSource("invalidPasswordBoundaries")
    void createRejectsEveryPasswordComplexityBoundary(String caseName, String password) {
        assertThatThrownBy(() -> admins.createAdmin(admin("invalid-" + caseName, password, null)))
                .isInstanceOf(InvalidArgumentException.class)
                .hasMessageContaining("Password must be 8 to 32 characters");
    }

    @Test
    void createDefaultsBlankHomePathToDashboard() {
        AdminDto missing = admin("home-missing", "Strong#123", List.of());
        missing.setHomePath(null);
        admins.createAdmin(missing);
        assertThat(admins.getAdminByUsername("home-missing").getHomePath())
                .isEqualTo("/dashboard");

        AdminDto blank = admin("home-blank", "Strong#123", List.of());
        blank.setHomePath("  ");
        admins.createAdmin(blank);
        assertThat(admins.getAdminByUsername("home-blank").getHomePath())
                .isEqualTo("/dashboard");
    }

    @Test
    void pageFiltersOrdersProfilesAndDeduplicatesRoleCodes() {
        ensureRole("operator");
        AdminDto first = admin("page-one", "Strong#123", List.of("operator"));
        first.setNickname("Team Alpha");
        first.setAvatar("images/2026/08/05/458c36d8-f4c3-4270-87f2-55518b553efd.png");
        first.setDesc("First operator");
        admins.createAdmin(first);
        AdminBo firstStored = admins.getAdminByUsername("page-one");

        AdminDto second = admin("page-two", "Strong#123", List.of("operator"));
        second.setNickname("Team Beta");
        admins.createAdmin(second);
        AdminDto disabled = admin("page-disabled", "Strong#123", List.of("operator"));
        disabled.setNickname("Team Disabled");
        disabled.setStatus(false);
        admins.createAdmin(disabled);

        Long operatorRoleId = roles.getRoleByCode("operator").getId();
        jdbc.update("""
                insert into az_user_role (del, create_time, user_id, role_id)
                values (0, now(), ?, ?)
                """, firstStored.getUserId(), operatorRoleId);

        AdminPageDto query = new AdminPageDto();
        query.setNickname("Team");
        query.setStatus(true);
        List<AdminVo> page = admins.getAdminPage(query).getList();

        assertThat(page).extracting(AdminVo::getUsername)
                .containsExactly("page-two", "page-one");
        assertThat(page.get(1).getAvatar()).isEqualTo(
                "images/2026/08/05/458c36d8-f4c3-4270-87f2-55518b553efd.png");
        assertThat(page.get(1).getAvatarUrl()).isNull();
        assertThat(page.get(1).getDesc()).isEqualTo("First operator");
        assertThat(page.get(1).getHomePath()).isEqualTo("/workspace");
        assertThat(page.get(1).getStatus()).isTrue();
        assertThat(page.get(1).getRoleCodes())
                .containsExactlyInAnyOrder("admin", "operator");
    }

    private AdminDto admin(String username, String password, List<String> roleCodes) {
        AdminDto dto = new AdminDto();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setNickname(username);
        dto.setHomePath("/workspace");
        dto.setStatus(true);
        dto.setRoleCodes(roleCodes);
        return dto;
    }

    private static Stream<Arguments> validPasswordBoundaries() {
        return Stream.of(
                Arguments.of("password-length-8", "Aa1#aaaa"),
                Arguments.of("password-length-32", "Aa1#" + "a".repeat(28)));
    }

    private static Stream<Arguments> invalidPasswordBoundaries() {
        return Stream.of(
                Arguments.of("length-7", "Aa1#aaa"),
                Arguments.of("length-33", "Aa1#" + "a".repeat(29)),
                Arguments.of("uppercase", "aa1#aaaa"),
                Arguments.of("lowercase", "AA1#AAAA"),
                Arguments.of("digit", "Aa#aaaaa"),
                Arguments.of("special", "Aa1aaaaa"),
                Arguments.of("space", "Aa1# aaa"),
                Arguments.of("tab", "Aa1#\taaa"),
                Arguments.of("newline", "Aa1#\naaa"));
    }

    private void ensureRole(String code) {
        if (roles.getRoleByCode(code) != null) {
            return;
        }
        RoleDto dto = new RoleDto();
        dto.setCode(code);
        dto.setName(code);
        roles.createRole(dto);
    }

    private void authenticateAs(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of(userId));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
