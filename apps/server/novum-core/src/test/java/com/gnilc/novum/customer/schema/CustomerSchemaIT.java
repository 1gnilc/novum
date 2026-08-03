package com.gnilc.novum.customer.schema;

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
class CustomerSchemaIT {
    @Container
    @ServiceConnection
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse(
            System.getProperty("app.test.mysql.image", "mysql:8.4.0")))
            .withDatabaseName("gnilc_customer_schema_test")
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
    void customerSchemaCreatesTheDefaultIdentityAndMandatoryRole() {
        runScript("sql/schema/02_admin.sql");
        runScript("sql/schema/08_customer.sql");

        assertThat(columnNames("nv_customer")).containsExactly(
                "id", "del", "create_time", "update_time", "user_id", "username",
                "password", "nickname", "avatar", "status");
        assertThat(indexNames("nv_customer")).containsExactlyInAnyOrder(
                "PRIMARY", "uk_username", "uk_user_id", "idx_status");
        assertThat(count("nv_customer", "username = 'customer' AND del = 0 AND status = 1"))
                .isEqualTo(1);
        assertThat(count("az_role", "code = 'customer' AND del = 0 AND built_in = 1"))
                .isEqualTo(1);
        assertThat(defaultRoleBindingCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM nv_customer customer
                  JOIN sys_admin admin ON admin.user_id = customer.user_id
                 WHERE customer.username = 'customer'
                   AND admin.username = 'admin'
                """, Integer.class)).isZero();
    }

    @Test
    void customerSchemaIsIdempotentAndRestoresManagedBaselineData() {
        runScript("sql/schema/08_customer.sql");
        Long userId = jdbc.queryForObject(
                "SELECT user_id FROM nv_customer WHERE username = 'customer'", Long.class);
        jdbc.update("UPDATE az_user_role SET del = 1 WHERE user_id = ?", userId);
        jdbc.update("UPDATE az_user SET del = 1 WHERE id = ?", userId);
        jdbc.update("UPDATE az_role SET del = 1, built_in = 0 WHERE code = 'customer'");
        jdbc.update("""
                UPDATE nv_customer
                   SET del = 1,
                       status = 0,
                       password = 'operator-managed-hash',
                       nickname = 'Operator Managed',
                       avatar = 'https://example.test/customer.png'
                 WHERE username = 'customer'
                """);

        runScript("sql/schema/08_customer.sql");
        runScript("sql/schema/08_customer.sql");

        assertThat(count("az_role", "code = 'customer' AND del = 0 AND built_in = 1"))
                .isEqualTo(1);
        assertThat(count("az_user", "id = " + userId + " AND del = 0")).isEqualTo(1);
        assertThat(defaultRoleBindingCount()).isEqualTo(1);
        assertThat(defaultRoleBindingTotalCount()).isEqualTo(1);
        assertThat(jdbc.queryForMap("""
                SELECT password, nickname, avatar, status, del
                  FROM nv_customer
                 WHERE username = 'customer'
                """))
                .containsEntry("password", "operator-managed-hash")
                .containsEntry("nickname", "Operator Managed")
                .containsEntry("avatar", "https://example.test/customer.png")
                .containsEntry("status", true)
                .containsEntry("del", 0);
    }

    @Test
    void customerPermissionsAreExactAndIdempotent() {
        runScript("sql/schema/08_customer.sql");
        runScript("sql/schema/09_customer_permissions.sql");
        runScript("sql/schema/09_customer_permissions.sql");

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE code IN (
                       'POST:/customer/login',
                       'POST:/customer/refresh',
                       'POST:/customer/logout')
                   AND public_access = 1
                   AND built_in = 1
                """, Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_permission
                 WHERE code = 'GET:/customer/user-info'
                   AND public_access = 0
                   AND built_in = 1
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_role_permission binding
                  JOIN az_role role ON role.id = binding.role_id
                  JOIN az_permission permission ON permission.id = binding.permission_id
                 WHERE role.code = 'customer'
                   AND permission.code = 'GET:/customer/user-info'
                   AND binding.del = 0
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM az_menu", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM az_role_menu", Integer.class)).isZero();
    }

    private java.util.List<String> columnNames(String table) {
        return jdbc.queryForList("""
                SELECT column_name
                  FROM information_schema.columns
                 WHERE table_schema = database()
                   AND table_name = ?
                 ORDER BY ordinal_position
                """, String.class, table);
    }

    private java.util.List<String> indexNames(String table) {
        return jdbc.queryForList("""
                SELECT DISTINCT index_name
                  FROM information_schema.statistics
                 WHERE table_schema = database()
                   AND table_name = ?
                """, String.class, table);
    }

    private int count(String table, String where) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + where, Integer.class);
    }

    private int defaultRoleBindingCount() {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user_role binding
                  JOIN nv_customer customer ON customer.user_id = binding.user_id
                  JOIN az_role role ON role.id = binding.role_id
                 WHERE customer.username = 'customer'
                   AND customer.del = 0
                   AND role.code = 'customer'
                   AND role.del = 0
                   AND binding.del = 0
                """, Integer.class);
    }

    private int defaultRoleBindingTotalCount() {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                  FROM az_user_role binding
                  JOIN nv_customer customer ON customer.user_id = binding.user_id
                  JOIN az_role role ON role.id = binding.role_id
                 WHERE customer.username = 'customer'
                   AND role.code = 'customer'
                """, Integer.class);
    }

    private void runScript(String path) {
        new ResourceDatabasePopulator(new ClassPathResource(path)).execute(dataSource);
    }

    private void dropSchema() {
        jdbc.execute("""
                DROP TABLE IF EXISTS
                    nv_customer,
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
