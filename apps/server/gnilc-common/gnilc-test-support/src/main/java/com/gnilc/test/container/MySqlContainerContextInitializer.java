package com.gnilc.test.container;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 启动共享 MySQL，并把动态连接信息注入 Spring 测试上下文。
 */
public final class MySqlContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * 配置当前测试上下文的数据源和测试清理开关。
     *
     * @param context 待初始化的 Spring 应用上下文
     */
    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
        SharedTestContainers.MySqlConnectionDetails mysql = SharedTestContainers.mysqlConnectionDetails();
        TestPropertyValues.of(
                "spring.datasource.url=" + mysql.getJdbcUrl(),
                "spring.datasource.username=" + mysql.getUsername(),
                "spring.datasource.password=" + mysql.getPassword(),
                "spring.datasource.driver-class-name=" + mysql.getDriverClassName(),
                "spring.sql.init.mode=never",
                "app.test.cleanup.enabled=true"
        ).applyTo(context);
    }
}
