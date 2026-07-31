package com.gnilc.novum.support;

import com.gnilc.test.container.SharedTestContainers;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public final class SystemModuleContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        SharedTestContainers.initializeMySqlSchema(
                "sql/schema/01_rbac.sql",
                "sql/schema/02_admin.sql",
                "sql/schema/05_admin_permissions.sql",
                "sql/schema/06_i18n.sql");
        TestPropertyValues.of(
                "server.servlet.context-path=/api",
                "spring.messages.basename=i18n/common/messages,i18n/rbac/messages,i18n/system/messages",
                "spring.messages.fallback-to-system-locale=false",
                "app.i18n.default-locale=en-US",
                "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                "mybatis-plus.global-config.db-config.logic-delete-field=del",
                "mybatis-plus.global-config.db-config.logic-delete-value=1",
                "mybatis-plus.global-config.db-config.logic-not-delete-value=0",
                "mybatis-plus.global-config.db-config.id-type=auto"
        ).applyTo(context);
    }
}
