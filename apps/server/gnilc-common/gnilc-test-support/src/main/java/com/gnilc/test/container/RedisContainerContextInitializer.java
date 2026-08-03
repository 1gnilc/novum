package com.gnilc.test.container;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 启动共享 Redis，并把动态连接信息注入 Spring 测试上下文。
 */
public final class RedisContainerContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * 配置当前测试上下文的 Redis 连接和测试清理开关。
     *
     * @param context 待初始化的 Spring 应用上下文
     */
    @Override
    public void initialize(@NotNull ConfigurableApplicationContext context) {
        SharedTestContainers.RedisConnectionDetails redis = SharedTestContainers.redisConnectionDetails();
        TestPropertyValues.of(
                "spring.data.redis.host=" + redis.getHost(),
                "spring.data.redis.port=" + redis.getPort(),
                "spring.data.redis.database=" + redis.getDatabase(),
                "app.test.cleanup.enabled=true"
        ).applyTo(context);
    }
}
