package com.gnilc.novum.admin.cache;

import com.gnilc.auth.authn.context.DefaultAccessPrincipal;
import com.gnilc.auth.authz.rbac.entity.bo.RoleBo;
import com.gnilc.auth.authz.rbac.entity.dto.RoleDto;
import com.gnilc.auth.authz.rbac.service.RoleService;
import com.gnilc.novum.admin.entity.bo.AdminBo;
import com.gnilc.novum.admin.entity.dto.AdminDto;
import com.gnilc.novum.admin.entity.vo.AdminVo;
import com.gnilc.novum.admin.service.AdminService;
import com.gnilc.novum.support.SystemContainerContextInitializer;
import com.gnilc.novum.support.SystemTestApplication;
import com.gnilc.test.cleanup.RedisCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SystemTestApplication.class)
@ActiveProfiles("test")
@ContextConfiguration(initializers = SystemContainerContextInitializer.class)
class AdminQueryCacheIT {
    private static final String USERNAME = "admin-query-cache";

    @Autowired
    private AdminService admins;
    @Autowired
    private RoleService roles;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private RedisConnectionFactory connectionFactory;
    @Autowired
    private JdbcTemplate jdbc;

    private Long adminId;
    private Long userId;

    @BeforeEach
    void setUp() {
        cleanRedis();
        cleanDatabase();
        ensureAdminRole();
        AdminDto create = new AdminDto();
        create.setUsername(USERNAME);
        create.setPassword("Strong#123");
        create.setNickname("Cached Before");
        create.setStatus(true);
        create.setRoleCodes(List.of("admin"));
        admins.createAdmin(create);
        AdminBo stored = admins.getAdminByUsername(USERNAME);
        adminId = stored.getId();
        userId = stored.getUserId();
        authenticate(userId);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        cleanRedis();
        cleanDatabase();
    }

    @Test
    void fourQueriesFillRedisAndProfileUpdateInvalidatesOnlyUserInfo() {
        AdminVo first = admins.getUserInfo();
        admins.getMenuAccessCodes(userId);
        admins.getMenuRoutes();

        assertThat(first.getNickname()).isEqualTo("Cached Before");
        assertThat(redis.hasKey(AdminCacheService.userInfoKey(userId))).isTrue();
        assertThat(redis.hasKey(AdminCacheService.roleCodesKey(userId))).isTrue();
        assertThat(redis.hasKey(AdminCacheService.menuAccessCodesKey(userId))).isTrue();
        assertThat(redis.hasKey(AdminCacheService.menuRoutesKey(userId))).isTrue();

        jdbc.update("update sys_admin set nickname = 'Bypassed Event' where id = ?", adminId);
        assertThat(admins.getUserInfo().getNickname()).isEqualTo("Cached Before");

        AdminDto profile = new AdminDto();
        profile.setNickname("Updated Through Service");
        admins.updateProfile(profile);

        assertThat(redis.hasKey(AdminCacheService.userInfoKey(userId))).isFalse();
        assertThat(redis.hasKey(AdminCacheService.roleCodesKey(userId))).isTrue();
        assertThat(redis.hasKey(AdminCacheService.menuAccessCodesKey(userId))).isTrue();
        assertThat(redis.hasKey(AdminCacheService.menuRoutesKey(userId))).isTrue();
        assertThat(admins.getUserInfo().getNickname()).isEqualTo("Updated Through Service");
    }

    private void ensureAdminRole() {
        RoleBo existing = roles.getRoleByCode("admin");
        if (existing != null) {
            return;
        }
        RoleDto role = new RoleDto();
        role.setCode("admin");
        role.setName("Administrator");
        roles.createRole(role);
    }

    private void authenticate(Long authenticatedUserId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setUserPrincipal(DefaultAccessPrincipal.of(authenticatedUserId));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void cleanRedis() {
        new RedisCleaner(connectionFactory).flushDatabase();
    }

    private void cleanDatabase() {
        List<Long> userIds = jdbc.queryForList(
                "select user_id from sys_admin where username = ?",
                Long.class,
                USERNAME);
        if (!userIds.isEmpty()) {
            jdbc.update("delete from az_user_role where user_id in (?)", userIds.get(0));
            jdbc.update("delete from az_user where id in (?)", userIds.get(0));
        }
        jdbc.update("delete from sys_admin where username = ?", USERNAME);
    }
}
