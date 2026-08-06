package com.gnilc.novum.customer.support;

import com.gnilc.auth.authz.rbac.provider.cache.PermissionCacheService;
import com.gnilc.test.cleanup.BaselineDataSeeder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@TestConfiguration(proxyBeanMethods = false)
public class CustomerApiTestConfiguration {
    private static final String[] BASELINE_SCRIPTS = {
            "02_admin.sql",
            "03_framework_permissions.sql",
            "04_rbac_permissions.sql",
            "05_admin_permissions.sql",
            "06_i18n.sql",
            "07_rbac_admin.sql",
            "08_customer.sql",
            "09_customer_permissions.sql",
            "10_image.sql"
    };

    @Bean
    BaselineDataSeeder customerApiBaselineDataSeeder(
            DataSource dataSource,
            PermissionCacheService cacheService) {
        return () -> {
            for (String script : BASELINE_SCRIPTS) {
                new ResourceDatabasePopulator(new ClassPathResource("sql/schema/" + script))
                        .execute(dataSource);
            }
            cacheService.resetAll();
        };
    }
}
