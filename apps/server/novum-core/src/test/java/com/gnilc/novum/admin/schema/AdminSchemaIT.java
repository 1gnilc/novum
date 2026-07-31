package com.gnilc.novum.admin.schema;

import com.gnilc.novum.support.SystemTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = NONE)
@ContextConfiguration(classes = SystemTestApplication.class)
@Testcontainers
@SuppressWarnings("resource")
class AdminSchemaIT {
    @Container
    @ServiceConnection
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse(
            System.getProperty("app.test.mysql.image", "mysql:8.4.0")))
            .withDatabaseName("gnilc_admin_schema_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void createRbacSchema() {
        dropSchema();
        runScript("sql/schema/01_rbac.sql");
    }

    @Test
    void adminSchemaCreatesTableAndDefaultAdminRelations() {
        runScript("sql/schema/02_admin.sql");

        assertThat(tableNames()).contains("sys_admin");
        assertThat(jdbc.queryForObject("""
                SELECT character_maximum_length
                  FROM information_schema.columns
                 WHERE table_schema = database()
                   AND table_name = 'sys_admin'
                   AND column_name = 'username'
                """, Integer.class)).isEqualTo(320);
        assertThat(count("sys_admin", "username = 'admin' AND del = 0")).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT home_path FROM sys_admin WHERE username = 'admin'", String.class))
                .isEqualTo("/dashboard");
        assertThat(jdbc.queryForObject("""
                SELECT column_default
                  FROM information_schema.columns
                 WHERE table_schema = database()
                   AND table_name = 'sys_admin'
                   AND column_name = 'home_path'
                """, String.class)).isEqualTo("/dashboard");
        assertThat(count("az_role", "code = 'admin' AND del = 0 AND built_in = 1")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user u
                  JOIN sys_admin a ON a.user_id = u.id
                 WHERE a.username = 'admin'
                   AND a.del = 0
                   AND u.del = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(defaultAdminRoleBindingCount()).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT name FROM az_menu WHERE del = 0 ORDER BY `order`", String.class))
                .containsExactly("Dashboard", "Profile");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM az_role_menu WHERE del = 0", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void adminSchemaCanRunRepeatedlyAndRestoreDefaultRelations() {
        runScript("sql/schema/02_admin.sql");
        runScript("sql/schema/02_admin.sql");

        assertThat(count("az_role", "code = 'admin' AND del = 0")).isEqualTo(1);
        assertThat(count("sys_admin", "username = 'admin' AND del = 0")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user u
                  JOIN sys_admin a ON a.user_id = u.id
                 WHERE a.username = 'admin'
                   AND a.del = 0
                   AND u.del = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(defaultAdminRoleBindingCount()).isEqualTo(1);
        Long userId = jdbc.queryForObject(
                "SELECT user_id FROM sys_admin WHERE username = 'admin'", Long.class);

        jdbc.update("""
                DELETE ur
                  FROM az_user_role ur
                  JOIN sys_admin a ON a.user_id = ur.user_id
                  JOIN az_role r ON r.id = ur.role_id
                 WHERE a.username = 'admin'
                   AND r.code = 'admin'
                """);
        runScript("sql/schema/02_admin.sql");

        assertThat(defaultAdminRoleBindingCount()).isEqualTo(1);

        jdbc.update("DELETE FROM az_user WHERE id = ?", userId);
        runScript("sql/schema/02_admin.sql");
        assertThat(count("az_user", "id = " + userId + " AND del = 0")).isEqualTo(1);

        jdbc.update("UPDATE az_user_role SET del = 1 WHERE user_id = ?", userId);
        jdbc.update("""
                UPDATE sys_admin
                   SET del = 1,
                       password = 'operator-managed-hash',
                       nickname = 'Operator Managed',
                       avatar = 'https://example.test/avatar.png',
                       description = 'Operator managed description',
                       home_path = '/operator-home',
                       status = 0
                 WHERE username = 'admin'
                """);
        jdbc.update("UPDATE az_user SET del = 1 WHERE id = ?", userId);
        jdbc.update("UPDATE az_role SET del = 1, built_in = 0 WHERE code = 'admin'");

        runScript("sql/schema/02_admin.sql");

        assertThat(count("az_role", "code = 'admin' AND del = 0 AND built_in = 1")).isEqualTo(1);
        assertThat(count("az_role", "code = 'admin'")).isEqualTo(1);
        assertThat(count("sys_admin", "username = 'admin' AND del = 0")).isEqualTo(1);
        assertThat(count("sys_admin", "username = 'admin'")).isEqualTo(1);
        assertThat(jdbc.queryForMap("""
                SELECT password, nickname, avatar, description, home_path, status
                  FROM sys_admin
                 WHERE username = 'admin'
                """))
                .containsEntry("password", "operator-managed-hash")
                .containsEntry("nickname", "Operator Managed")
                .containsEntry("avatar", "https://example.test/avatar.png")
                .containsEntry("description", "Operator managed description")
                .containsEntry("home_path", "/operator-home")
                .containsEntry("status", true);
        assertThat(count("az_user", "id = " + userId + " AND del = 0")).isEqualTo(1);
        assertThat(defaultAdminRoleBindingCount()).isEqualTo(1);
    }

    @Test
    void adminSchemaRestoresBaselineRoleForEveryActiveAdmin() {
        runScript("sql/schema/02_admin.sql");
        Long adminRoleId = jdbc.queryForObject(
                "SELECT id FROM az_role WHERE code = 'admin'", Long.class);
        jdbc.update("INSERT INTO az_user (del, create_time) VALUES (0, NOW())");
        Long userId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO sys_admin
                    (del, create_time, user_id, username, password, nickname, home_path, status)
                VALUES (0, NOW(), ?, 'existing', 'hash', 'Existing', '/workspace', 1)
                """, userId);
        jdbc.update("""
                INSERT INTO az_user_role (del, create_time, user_id, role_id)
                VALUES (1, NOW(), ?, ?)
                """, userId, adminRoleId);

        runScript("sql/schema/02_admin.sql");

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user_role
                 WHERE user_id = ?
                   AND role_id = ?
                   AND del = 0
                """, Integer.class, userId, adminRoleId)).isEqualTo(1);
    }

    @Test
    void adminPermissionsCanRunRepeatedly() {
        runScript("sql/schema/02_admin.sql");
        runScript("sql/schema/05_admin_permissions.sql");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM az_permission", Integer.class))
                .isEqualTo(14);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE code = name
                   AND code = CONCAT(target_qualifier, ':', target_identifier)
                   AND public_access = 1
                """, Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE code IN (
                       'GET:/sys/admin/user-info',
                       'GET:/sys/admin/role-codes',
                       'GET:/sys/admin/menu/access-codes',
                       'GET:/sys/admin/menu/routes',
                       'POST:/sys/admin/user-info/update',
                       'POST:/sys/admin/password/update')
                   AND public_access = 0
                """, Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_permission rp
                  JOIN az_role r ON r.id = rp.role_id
                  JOIN az_permission p ON p.id = rp.permission_id
                 WHERE r.code = 'admin'
                   AND r.del = 0
                   AND rp.del = 0
                   AND p.code IN (
                       'GET:/sys/admin/user-info',
                       'GET:/sys/admin/role-codes',
                       'GET:/sys/admin/menu/access-codes',
                       'GET:/sys/admin/menu/routes',
                       'POST:/sys/admin/user-info/update',
                       'POST:/sys/admin/password/update')
                """, Integer.class)).isEqualTo(6);

        jdbc.update("""
                UPDATE az_permission
                   SET name = 'Operator managed',
                       public_access = 0
                 WHERE code = 'POST:/sys/admin/remove/{id}'
                """);
        runScript("sql/schema/05_admin_permissions.sql");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM az_permission", Integer.class))
                .isEqualTo(14);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE code = 'POST:/sys/admin/remove/{id}'
                   AND name = 'Operator managed'
                   AND public_access = 0
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void rbacAdminSchemaCreatesProtectedManagementResourcesAndCanRunRepeatedly() {
        runScriptsThroughI18n();
        runScript("sql/schema/07_rbac_admin.sql");

        assertThat(count("az_role", "code = 'rbac:manager' AND del = 0 AND built_in = 1"))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE del = 0
                   AND public_access = 0
                   AND built_in = 1
                   AND (target_identifier LIKE '/authz/%'
                        OR code IN (
                            'POST:/sys/admin/page',
                            'POST:/sys/admin/create',
                            'POST:/sys/admin/update',
                            'POST:/sys/admin/roles/save',
                            'POST:/sys/admin/remove/{id}'))
                """, Integer.class)).isEqualTo(25);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_permission rp
                  JOIN az_role r ON r.id = rp.role_id
                  JOIN az_permission p ON p.id = rp.permission_id
                 WHERE r.code = 'rbac:manager'
                   AND r.del = 0
                   AND rp.del = 0
                   AND p.del = 0
                """, Integer.class)).isEqualTo(27);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_permission rp
                  JOIN az_role r ON r.id = rp.role_id
                  JOIN az_permission p ON p.id = rp.permission_id
                 WHERE r.code = 'rbac:manager'
                   AND r.del = 0
                   AND rp.del = 0
                   AND p.del = 0
                   AND p.code IN (
                       'POST:/sys/i18n-message/values/{messageKey}',
                       'POST:/sys/i18n-message/save')
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_menu
                 WHERE name IN ('Admin', 'Role', 'Permission', 'Menu')
                   AND type = 'menu'
                   AND del = 0
                   AND built_in = 1
                """, Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_menu
                WHERE type = 'button'
                   AND access_code IS NOT NULL
                   AND del = 0
                   AND built_in = 1
                """, Integer.class)).isEqualTo(17);
        assertThat(jdbc.queryForList("""
                SELECT CONCAT(name, ':', access_code)
                  FROM az_menu
                 WHERE name IN (
                     'AdminCreate', 'AdminUpdate', 'AdminRemove', 'AdminRole',
                     'RoleCreate', 'RoleUpdate', 'RoleRemove', 'RolePermission', 'RoleMenu',
                     'PermissionCreate', 'PermissionUpdate', 'PermissionRemove',
                     'MenuCreate', 'MenuUpdate', 'MenuRemove')
                   AND del = 0
                """, String.class)).containsExactlyInAnyOrder(
                        "AdminCreate:system:admin:create",
                        "AdminUpdate:system:admin:update",
                        "AdminRemove:system:admin:remove",
                        "AdminRole:system:admin:manage-roles",
                        "RoleCreate:system:role:create",
                        "RoleUpdate:system:role:update",
                        "RoleRemove:system:role:remove",
                        "RolePermission:system:role:manage-permissions",
                        "RoleMenu:system:role:manage-menus",
                        "PermissionCreate:system:permission:create",
                        "PermissionUpdate:system:permission:update",
                        "PermissionRemove:system:permission:remove",
                        "MenuCreate:system:menu:create",
                        "MenuUpdate:system:menu:update",
                        "MenuRemove:system:menu:remove");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_menu rm
                  JOIN az_role r ON r.id = rm.role_id
                  JOIN az_menu m ON m.id = rm.menu_id
                 WHERE r.code = 'rbac:manager'
                   AND r.del = 0
                   AND rm.del = 0
                   AND m.del = 0
                """, Integer.class)).isEqualTo(20);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user_role ur
                  JOIN sys_admin a ON a.user_id = ur.user_id
                  JOIN az_role r ON r.id = ur.role_id
                 WHERE a.username = 'admin'
                   AND a.del = 0
                   AND r.code = 'rbac:manager'
                   AND r.del = 0
                   AND ur.del = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM sys_i18n
                 WHERE category = 'admin'
                   AND message_key IN (
                       'menu.system.admin.title',
                       'menu.system.role.title',
                       'menu.system.permission.title',
                       'menu.system.menu.title')
                """, Integer.class)).isEqualTo(8);

        jdbc.update("""
                UPDATE sys_i18n
                   SET category = 'default',
                       i18n_value = CASE
                           WHEN locale = 'en-US' THEN 'Operator managed'
                           ELSE i18n_value
                       END
                 WHERE message_key = 'menu.system.admin.title'
                """);
        jdbc.update("""
                DELETE FROM sys_i18n
                 WHERE message_key = 'menu.system.admin.title'
                   AND locale = 'zh-CN'
                """);
        runScript("sql/schema/07_rbac_admin.sql");

        assertThat(count("az_role", "code = 'rbac:manager' AND del = 0")).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT i18n_value
                  FROM sys_i18n
                WHERE category = 'default'
                   AND message_key = 'menu.system.admin.title'
                   AND locale = 'en-US'
                """, String.class)).isEqualTo("Operator managed");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM sys_i18n
                 WHERE category = 'default'
                   AND message_key = 'menu.system.admin.title'
                """, Integer.class)).isEqualTo(2);
    }

    @Test
    void rbacAdminSchemaMigratesLegacyManagerRoleCodeWithoutChangingIdentity() {
        runScriptsThroughI18n();
        jdbc.update("""
                INSERT INTO az_role (
                    del, create_time, update_time, code, name, remark, built_in)
                VALUES (
                    0, NOW(), NULL, 'rbac-manager', 'RBAC 管理员',
                    'Legacy role code', 1)
                """);
        Long roleId = jdbc.queryForObject(
                "SELECT id FROM az_role WHERE code = 'rbac-manager'", Long.class);

        runScript("sql/schema/07_rbac_admin.sql");

        assertThat(jdbc.queryForObject("""
                SELECT id FROM az_role
                 WHERE code = 'rbac:manager' AND del = 0
                """, Long.class)).isEqualTo(roleId);
        assertThat(count("az_role", "code = 'rbac-manager' AND del = 0")).isZero();
    }

    @Test
    void rbacAdminSchemaAddsBuiltInColumnsToPreviousSchema() {
        jdbc.execute("ALTER TABLE az_permission DROP COLUMN built_in");
        jdbc.execute("ALTER TABLE az_menu DROP COLUMN built_in");
        runScriptsThroughI18n();

        runScript("sql/schema/07_rbac_admin.sql");

        assertThat(columnNames("az_permission")).contains("built_in");
        assertThat(columnNames("az_menu")).contains("built_in");
        assertThat(count("az_role", "code = 'rbac:manager' AND del = 0 AND built_in = 1"))
                .isEqualTo(1);
    }

    @Test
    void rbacAdminSchemaMigratesAssignmentSaveRoutesWithoutLosingGrants() {
        runScriptsThroughI18n();
        jdbc.update("""
                UPDATE az_permission
                   SET name = CASE code
                           WHEN 'POST:/sys/admin/roles/save' THEN 'POST:/sys/admin/update-roles'
                           WHEN 'POST:/authz/role-permission/save' THEN 'POST:/authz/role-permission/update'
                           WHEN 'POST:/authz/role-menu/save' THEN 'POST:/authz/role-menu/update'
                       END,
                       target_identifier = CASE code
                           WHEN 'POST:/sys/admin/roles/save' THEN '/sys/admin/update-roles'
                           WHEN 'POST:/authz/role-permission/save' THEN '/authz/role-permission/update'
                           WHEN 'POST:/authz/role-menu/save' THEN '/authz/role-menu/update'
                       END,
                       code = CASE code
                           WHEN 'POST:/sys/admin/roles/save' THEN 'POST:/sys/admin/update-roles'
                           WHEN 'POST:/authz/role-permission/save' THEN 'POST:/authz/role-permission/update'
                           WHEN 'POST:/authz/role-menu/save' THEN 'POST:/authz/role-menu/update'
                       END
                 WHERE code IN (
                       'POST:/sys/admin/roles/save',
                       'POST:/authz/role-permission/save',
                       'POST:/authz/role-menu/save')
                """);
        jdbc.update("""
                INSERT INTO az_role_permission
                    (del, create_time, update_time, role_id, permission_id)
                SELECT 0, NOW(), NULL, role.id, permission.id
                  FROM az_role role
                  JOIN az_permission permission ON permission.code IN (
                       'POST:/sys/admin/update-roles',
                       'POST:/authz/role-permission/update',
                       'POST:/authz/role-menu/update')
                 WHERE role.code = 'admin'
                """);

        runScript("sql/schema/07_rbac_admin.sql");

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE del = 0
                   AND code IN (
                       'POST:/sys/admin/update-roles',
                       'POST:/authz/role-permission/update',
                       'POST:/authz/role-menu/update')
                """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_permission binding
                  JOIN az_role role ON role.id = binding.role_id
                  JOIN az_permission permission ON permission.id = binding.permission_id
                 WHERE role.code = 'admin'
                   AND binding.del = 0
                   AND permission.del = 0
                   AND permission.code IN (
                       'POST:/sys/admin/roles/save',
                       'POST:/authz/role-permission/save',
                       'POST:/authz/role-menu/save')
                """, Integer.class)).isEqualTo(3);
    }

    private java.util.List<String> tableNames() {
        return jdbc.queryForList("""
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = database()
                """, String.class);
    }

    private java.util.List<String> columnNames(String table) {
        return jdbc.queryForList("""
                SELECT column_name
                  FROM information_schema.columns
                 WHERE table_schema = database()
                   AND table_name = ?
                """, String.class, table);
    }

    private void runScriptsThroughI18n() {
        runScript("sql/schema/02_admin.sql");
        runScript("sql/schema/03_framework_permissions.sql");
        runScript("sql/schema/04_rbac_permissions.sql");
        runScript("sql/schema/05_admin_permissions.sql");
        runScript("sql/schema/06_i18n.sql");
    }

    private int count(String table, String where) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Integer.class);
    }

    private int defaultAdminRoleBindingCount() {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user_role ur
                  JOIN sys_admin a ON a.user_id = ur.user_id
                  JOIN az_role r ON r.id = ur.role_id
                 WHERE a.username = 'admin'
                   AND a.del = 0
                   AND r.code = 'admin'
                   AND r.del = 0
                   AND ur.del = 0
                """, Integer.class);
    }

    private void runScript(String path) {
        new ResourceDatabasePopulator(new ClassPathResource(path)).execute(dataSource);
    }

    private void dropSchema() {
        jdbc.execute("""
                DROP TABLE IF EXISTS
                    sys_admin,
                    az_role_menu,
                    az_role_permission,
                    az_user_role,
                    az_menu,
                    az_permission,
                    az_user,
                    az_role
                """);
    }
}
