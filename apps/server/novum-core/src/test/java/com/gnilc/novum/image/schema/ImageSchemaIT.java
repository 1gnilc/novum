package com.gnilc.novum.image.schema;

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
class ImageSchemaIT {
    @Container
    @ServiceConnection
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse(
            System.getProperty("app.test.mysql.image", "mysql:8.4.0")))
            .withDatabaseName("novum_image_schema_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void createBaseline() {
        jdbc.execute("""
                DROP TABLE IF EXISTS
                    sys_image, nv_customer, sys_i18n, sys_admin,
                    az_role_menu, az_role_permission, az_user_role,
                    az_menu, az_permission, az_user, az_role
                """);
        for (int index = 1; index <= 9; index++) {
            runScript(String.format("sql/schema/%02d_%s.sql", index, scriptName(index)));
        }
    }

    @Test
    void imageSchemaAndAuthorizationAreIdempotent() {
        runScript("sql/schema/10_image.sql");
        runScript("sql/schema/10_image.sql");

        assertThat(columns("sys_image")).containsExactly(
                "id", "del", "create_time", "update_time", "object_key",
                "content_type", "content_length", "status", "expires_at");
        assertThat(count("az_role", "code = 'image:manager' AND built_in = 1 AND del = 0"))
                .isEqualTo(1);
        assertThat(count("az_permission", "code IN ('POST:/image/presign', 'POST:/image/finalize', "
                + "'POST:/image/page', 'POST:/image/remove/{id}') AND built_in = 1 AND del = 0"))
                .isEqualTo(4);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM az_role_permission binding
                JOIN az_role role ON role.id = binding.role_id AND role.del = 0
                JOIN az_permission permission ON permission.id = binding.permission_id AND permission.del = 0
                WHERE binding.del = 0
                  AND ((role.code IN ('admin', 'customer')
                        AND permission.code IN ('POST:/image/presign', 'POST:/image/finalize'))
                    OR (role.code = 'image:manager'
                        AND permission.code IN ('POST:/image/page', 'POST:/image/remove/{id}')))
                """, Integer.class)).isEqualTo(6);
        assertThat(count("az_menu", "name IN ('Image', 'ImageRemove') AND built_in = 1 AND del = 0"))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM az_user_role binding
                JOIN sys_admin admin ON admin.user_id = binding.user_id AND admin.username = 'admin'
                JOIN az_role role ON role.id = binding.role_id AND role.code = 'image:manager'
                WHERE binding.del = 0
                """, Integer.class)).isEqualTo(1);
    }

    private java.util.List<String> columns(String table) {
        return jdbc.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = database() AND table_name = ?
                ORDER BY ordinal_position
                """, String.class, table);
    }

    private int count(String table, String where) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Integer.class);
    }

    private void runScript(String path) {
        new ResourceDatabasePopulator(new ClassPathResource(path)).execute(dataSource);
    }

    private static String scriptName(int index) {
        return switch (index) {
            case 1 -> "rbac";
            case 2 -> "admin";
            case 3 -> "framework_permissions";
            case 4 -> "rbac_permissions";
            case 5 -> "admin_permissions";
            case 6 -> "i18n";
            case 7 -> "rbac_admin";
            case 8 -> "customer";
            case 9 -> "customer_permissions";
            default -> throw new IllegalArgumentException();
        };
    }
}
