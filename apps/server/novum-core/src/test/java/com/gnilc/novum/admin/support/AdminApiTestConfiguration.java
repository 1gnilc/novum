package com.gnilc.novum.admin.support;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheService;
import com.gnilc.test.cleanup.BaselineDataSeeder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class AdminApiTestConfiguration {
    static final String LIMITED_USERNAME = "limited";
    static final String LIMITED_PASSWORD = "123456";
    private static final long LIMITED_USER_ID = 900_001L;
    private static final String DEFAULT_PASSWORD_HASH =
            "$2y$10$vjUNB/mAmPcweognGYbnyOeeQQzjL5DCQeThxucH1pC6nJfskup7G";
    @Bean
    BaselineDataSeeder adminApiBaselineDataSeeder(DataSource dataSource,
                                                  JdbcTemplate jdbc,
                                                  PermissionCacheService cacheService) {
        return () -> {
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/02_admin.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/03_framework_permissions.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/04_rbac_permissions.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/05_admin_permissions.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/06_i18n.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/07_rbac_admin.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/08_customer.sql"))
                    .execute(dataSource);
            new ResourceDatabasePopulator(new ClassPathResource("sql/schema/09_customer_permissions.sql"))
                    .execute(dataSource);
            jdbc.update("""
                    insert into az_role (del, create_time, code, name, built_in)
                    values (0, now(), 'limited', 'Limited', 0)
                    """);
            Long limitedRoleId = jdbc.queryForObject(
                    "select id from az_role where code = 'limited' and del = 0", Long.class);
            jdbc.update("insert into az_user (id, del, create_time) values (?, 0, now())", LIMITED_USER_ID);
            jdbc.update("""
                    insert into sys_admin
                        (del, create_time, user_id, username, password, nickname, home_path, status)
                    values (0, now(), ?, ?, ?, 'Limited', '/workspace', 1)
                    """, LIMITED_USER_ID, LIMITED_USERNAME, DEFAULT_PASSWORD_HASH);
            jdbc.update("""
                    insert into az_user_role (del, create_time, user_id, role_id)
                    values (0, now(), ?, ?)
                    """, LIMITED_USER_ID, limitedRoleId);
            cacheService.resetAll();
        };
    }
}
