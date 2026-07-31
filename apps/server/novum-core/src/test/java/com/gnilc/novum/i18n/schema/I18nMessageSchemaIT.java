package com.gnilc.novum.i18n.schema;

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
class I18nMessageSchemaIT {

    @Container
    @ServiceConnection
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse(
            System.getProperty("app.test.mysql.image", "mysql:8.4.0")))
            .withDatabaseName("gnilc_i18n_schema_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void createBaseSchema() {
        dropSchema();
        runScript("sql/schema/01_rbac.sql");
        runScript("sql/schema/02_admin.sql");
        runScript("sql/schema/05_admin_permissions.sql");
    }

    @Test
    void i18nMessageSchemaIsIdempotentAndSeedsMenusRolesAndPermissions() {
        jdbc.update("""
                INSERT INTO az_role (
                    del, create_time, update_time, code, name, remark, built_in)
                VALUES (
                    0, NOW(), NULL, 'i18n:manager', '国际化配置管理员',
                    '维护客户端动态国际化配置', 1)
                """);
        runScript("sql/schema/06_i18n.sql");
        jdbc.update("""
                UPDATE sys_i18n
                   SET category = 'default'
                 WHERE message_key = 'menu.dashboard.title'
                """);
        jdbc.update("""
                DELETE FROM sys_i18n
                 WHERE message_key = 'menu.dashboard.title'
                   AND locale = 'zh-CN'
                """);
        runScript("sql/schema/06_i18n.sql");

        assertThat(jdbc.queryForList("""
                SELECT column_name
                  FROM information_schema.columns
                 WHERE table_schema = database()
                   AND table_name = 'sys_i18n'
                 ORDER BY ordinal_position
                """, String.class)).containsExactly(
                        "id", "category", "message_key", "locale", "i18n_value",
                        "create_time", "update_time");
        assertThat(jdbc.queryForList("""
                SELECT column_name
                  FROM information_schema.statistics
                 WHERE table_schema = database()
                   AND table_name = 'sys_i18n'
                   AND index_name = 'uk_message_key_locale'
                 ORDER BY seq_in_index
                """, String.class)).containsExactly("message_key", "locale");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys_i18n", Integer.class))
                .isEqualTo(12);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_i18n
                 WHERE category = 'default'
                   AND message_key = 'menu.dashboard.title'
                """, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForList("""
                SELECT title FROM az_menu
                 WHERE name IN ('Dashboard', 'Profile', 'System', 'I18nMessage') AND del = 0
                 ORDER BY name
                """, String.class)).containsExactlyInAnyOrder(
                        "menu.dashboard.title",
                        "menu.profile.title",
                        "menu.system.title",
                        "menu.i18nMessage.title");
        assertThat(jdbc.queryForObject("""
                SELECT component FROM az_menu
                 WHERE name = 'System' AND del = 0
                """, String.class)).isEqualTo("BasicLayout");
        assertThat(jdbc.queryForList("""
                SELECT CONCAT(name, ':', access_code)
                  FROM az_menu
                 WHERE name IN ('I18nMessageSave', 'I18nMessageRemove')
                   AND del = 0
                   AND type = 'button'
                ORDER BY name
                """, String.class)).containsExactly(
                        "I18nMessageRemove:system:i18n-message:remove",
                        "I18nMessageSave:system:i18n-message:save");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM az_role
                 WHERE code = 'i18n:manager' AND del = 0 AND built_in = 1
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT remark FROM az_role
                 WHERE code = 'i18n:manager'
                """, String.class)).isEqualTo("跨分类维护动态国际化配置");
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user_role ur
                  JOIN sys_admin a ON a.user_id = ur.user_id
                  JOIN az_role r ON r.id = ur.role_id
                 WHERE a.username = 'admin'
                   AND r.code = 'i18n:manager'
                   AND ur.del = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM az_permission
                 WHERE code LIKE 'POST:/sys/i18n-message/%' AND public_access = 0
                """, Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_permission rp
                  JOIN az_role r ON r.id = rp.role_id
                  JOIN az_permission p ON p.id = rp.permission_id
                 WHERE r.code = 'admin'
                   AND p.code = 'POST:/sys/i18n-message/bundle/{category}'
                   AND rp.del = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_permission rp
                  JOIN az_role r ON r.id = rp.role_id
                  JOIN az_permission p ON p.id = rp.permission_id
                 WHERE r.code = 'i18n:manager'
                   AND p.code IN (
                       'POST:/sys/i18n-message/categories',
                       'POST:/sys/i18n-message/page',
                       'POST:/sys/i18n-message/values/{messageKey}',
                       'POST:/sys/i18n-message/create',
                       'POST:/sys/i18n-message/save',
                       'POST:/sys/i18n-message/remove/{messageKey}')
                   AND rp.del = 0
                """, Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_menu rm
                  JOIN az_role r ON r.id = rm.role_id
                  JOIN az_menu m ON m.id = rm.menu_id
                 WHERE r.code = 'i18n:manager'
                   AND m.name IN (
                       'System',
                       'I18nMessage',
                       'I18nMessageSave',
                       'I18nMessageRemove')
                   AND rm.del = 0
                """, Integer.class)).isEqualTo(4);
    }

    @Test
    void i18nMessageSchemaMigratesLegacyManagerRoleCodeWithoutChangingIdentity() {
        jdbc.update("""
                INSERT INTO az_role (
                    del, create_time, update_time, code, name, remark, built_in)
                VALUES (
                    0, NOW(), NULL, 'i18n-manager', '国际化配置管理员',
                    'Legacy role code', 1)
                """);
        Long roleId = jdbc.queryForObject(
                "SELECT id FROM az_role WHERE code = 'i18n-manager'", Long.class);

        runScript("sql/schema/06_i18n.sql");

        assertThat(jdbc.queryForObject("""
                SELECT id FROM az_role
                 WHERE code = 'i18n:manager' AND del = 0
                """, Long.class)).isEqualTo(roleId);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM az_role
                 WHERE code = 'i18n-manager' AND del = 0
                """, Integer.class)).isZero();
    }

    @Test
    void i18nMessageSchemaMigratesPreviousClientColumnAndIndexes() {
        jdbc.execute("""
                CREATE TABLE sys_i18n (
                    id bigint NOT NULL AUTO_INCREMENT,
                    client varchar(64) NOT NULL,
                    message_key varchar(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
                    locale varchar(20) NOT NULL,
                    i18n_value text NOT NULL,
                    create_time datetime NOT NULL,
                    update_time datetime DEFAULT NULL,
                    PRIMARY KEY (id),
                    UNIQUE KEY uk_message_key_locale_client (message_key, locale, client),
                    KEY idx_message_key_client (message_key, client),
                    KEY idx_client_locale_key (client, locale, message_key)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        jdbc.update("""
                INSERT INTO sys_i18n
                    (client, message_key, locale, i18n_value, create_time)
                VALUES
                    ('portal', 'existing.message', 'en-US', 'Portal', NOW()),
                    ('admin', 'existing.message', 'en-US', 'Admin', NOW()),
                    ('portal', 'existing.message', 'zh-CN', '现有消息', NOW()),
                    ('portal', 'other.message', 'en-US', 'Other', NOW())
                """);

        runScript("sql/schema/06_i18n.sql");

        assertThat(jdbc.queryForList("""
                SELECT column_name
                  FROM information_schema.columns
                 WHERE table_schema = database()
                   AND table_name = 'sys_i18n'
                 ORDER BY ordinal_position
                """, String.class)).containsExactly(
                        "id", "category", "message_key", "locale", "i18n_value",
                        "create_time", "update_time");
        assertThat(jdbc.queryForList("""
                SELECT DISTINCT index_name
                  FROM information_schema.statistics
                 WHERE table_schema = database()
                   AND table_name = 'sys_i18n'
                """, String.class)).contains(
                        "uk_message_key_locale",
                        "idx_category_message_key",
                        "idx_category_locale_key")
                .doesNotContain(
                        "uk_message_key_locale_client",
                        "idx_message_key_client",
                        "idx_client_locale_key");
        assertThat(jdbc.queryForList("""
                SELECT CONCAT(category, ':', locale, ':', i18n_value)
                  FROM sys_i18n
                 WHERE message_key = 'existing.message'
                 ORDER BY locale
                """, String.class)).containsExactly(
                        "admin:en-US:Admin",
                        "admin:zh-CN:现有消息");
        assertThat(jdbc.queryForObject("""
                SELECT category
                  FROM sys_i18n
                 WHERE message_key = 'other.message'
                """, String.class)).isEqualTo("default");
    }

    private void runScript(String path) {
        new ResourceDatabasePopulator(new ClassPathResource(path)).execute(dataSource);
    }

    private void dropSchema() {
        jdbc.execute("""
                DROP TABLE IF EXISTS
                    sys_i18n,
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
