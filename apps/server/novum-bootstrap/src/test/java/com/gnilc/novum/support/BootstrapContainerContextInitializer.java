package com.gnilc.novum.support;

import com.gnilc.test.container.FullStackContainerContextInitializer;
import com.gnilc.test.container.SharedTestContainers;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public final class BootstrapContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        new FullStackContainerContextInitializer().initialize(context);
        SharedTestContainers.initializeMySqlSchema(
                "sql/schema/01_rbac.sql",
                "sql/schema/02_admin.sql",
                "sql/schema/03_framework_permissions.sql",
                "sql/schema/04_rbac_permissions.sql",
                "sql/schema/05_admin_permissions.sql",
                "sql/schema/06_i18n.sql",
                "sql/schema/07_rbac_admin.sql");
        TestPropertyValues.of(
                "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                "mybatis-plus.global-config.db-config.logic-delete-field=del",
                "mybatis-plus.global-config.db-config.logic-delete-value=1",
                "mybatis-plus.global-config.db-config.logic-not-delete-value=0",
                "mybatis-plus.global-config.db-config.id-type=auto"
        ).applyTo(context);
    }
}
