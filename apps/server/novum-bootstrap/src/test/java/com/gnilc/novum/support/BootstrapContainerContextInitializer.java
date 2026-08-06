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
                "sql/schema/07_rbac_admin.sql",
                "sql/schema/08_customer.sql",
                "sql/schema/09_customer_permissions.sql",
                "sql/schema/10_image.sql");
        TestPropertyValues.of(
                "app.s3.endpoint=http://127.0.0.1:9",
                "app.s3.region=auto",
                "app.s3.bucket=test-images",
                "app.s3.access-key=test-access-key",
                "app.s3.secret-key=test-secret-key",
                "app.s3.public-base-url=https://images.example.test",
                "app.s3.cleanup-cron=-",
                "mybatis-plus.configuration.map-underscore-to-camel-case=true",
                "mybatis-plus.global-config.db-config.logic-delete-field=del",
                "mybatis-plus.global-config.db-config.logic-delete-value=1",
                "mybatis-plus.global-config.db-config.logic-not-delete-value=0",
                "mybatis-plus.global-config.db-config.id-type=auto"
        ).applyTo(context);
    }
}
