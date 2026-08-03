package com.gnilc.test.cleanup;

import com.gnilc.test.container.SharedTestContainers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 为需要显式数据重置的测试注册清理、保护和基线恢复组件。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestCleanupConfiguration {
    /** 创建仅允许操作当前共享容器端点的清理保护器。 */
    @Bean
    TestEnvironmentGuard testEnvironmentGuard(Environment environment,
                                              DataSource dataSource,
                                              RedisConnectionFactory redisConnectionFactory) {
        SharedTestContainers.MySqlConnectionDetails mysql = SharedTestContainers.mysqlConnectionDetails();
        SharedTestContainers.RedisConnectionDetails redis = SharedTestContainers.redisConnectionDetails();
        return new TestEnvironmentGuard(
                environment,
                dataSource,
                redisConnectionFactory,
                mysql.getJdbcUrl(),
                redis.getHost(),
                redis.getPort(),
                redis.getDatabase());
    }

    /** 创建 MySQL 业务表清理器。 */
    @Bean
    DatabaseCleaner databaseCleaner(JdbcTemplate jdbcTemplate) {
        return new DatabaseCleaner(jdbcTemplate);
    }

    /** 创建 Redis 当前数据库清理器。 */
    @Bean
    RedisCleaner redisCleaner(RedisConnectionFactory connectionFactory) {
        return new RedisCleaner(connectionFactory);
    }

    /** 创建负责清理并恢复应用测试基线的编排器。 */
    @Bean
    TestDataResetManager testDataResetManager(TestEnvironmentGuard guard,
                                              DatabaseCleaner databaseCleaner,
                                              RedisCleaner redisCleaner,
                                              ObjectProvider<BaselineDataSeeder> seeders) {
        return new TestDataResetManager(guard, databaseCleaner, redisCleaner, seeders.orderedStream().toList());
    }
}
