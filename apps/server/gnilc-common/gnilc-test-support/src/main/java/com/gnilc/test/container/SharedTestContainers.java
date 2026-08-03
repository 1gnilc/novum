package com.gnilc.test.container;

import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Driver;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 管理集成测试进程内共享的 MySQL 和 Redis Testcontainers 实例。
 *
 * <p>容器生命周期与测试 JVM 一致，不能在单个初始化器或测试类中关闭；
 * Testcontainers Ryuk 会在 JVM 退出时回收容器。</p>
 */
@SuppressWarnings("resource")
public final class SharedTestContainers {
    /** 测试 MySQL 实例使用的固定数据库名。 */
    public static final String DATABASE_NAME = "gnilc_auth_test";
    private static final int REDIS_DATABASE = 0;

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse(System.getProperty("app.test.mysql.image", "mysql:8.4.0")))
            .withDatabaseName(DATABASE_NAME)
            .withUsername("test")
            .withPassword("test")
            .withEnv("TZ", "Asia/Shanghai");

    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse(System.getProperty("app.test.redis.image", "redis:8-alpine")))
            .withExposedPorts(6379);

    private static final Set<String> INITIALIZED_SCHEMAS = new HashSet<>();

    private SharedTestContainers() {
    }

    /**
     * 启动共享 MySQL，并返回供 Spring 测试上下文使用的不可关闭连接信息。
     *
     * @return 当前共享 MySQL 的连接信息
     */
    public static synchronized MySqlConnectionDetails mysqlConnectionDetails() {
        ensureMySqlRunning();
        return new MySqlConnectionDetails(
                utcJdbcUrl(MYSQL.getJdbcUrl()), MYSQL.getUsername(), MYSQL.getPassword(), MYSQL.getDriverClassName());
    }

    /**
     * 启动共享 Redis，并返回供 Spring 测试上下文使用的不可关闭连接信息。
     *
     * @return 当前共享 Redis 的连接信息
     */
    public static synchronized RedisConnectionDetails redisConnectionDetails() {
        ensureRedisRunning();
        return new RedisConnectionDetails(REDIS.getHost(), REDIS.getMappedPort(6379), REDIS_DATABASE);
    }

    /**
     * 使用测试类路径中的部署脚本初始化共享 MySQL schema。
     * 同一组脚本在一个测试 JVM 内只执行一次。
     *
     * @param classpathLocations 按执行顺序排列的类路径资源
     */
    public static synchronized void initializeMySqlSchema(String... classpathLocations) {
        ensureMySqlRunning();
        String schemaKey = String.join("\n", classpathLocations);
        if (!INITIALIZED_SCHEMAS.add(schemaKey)) {
            return;
        }
        try {
            Driver driver = (Driver) Class.forName(MYSQL.getDriverClassName()).getDeclaredConstructor().newInstance();
            SimpleDriverDataSource dataSource = new SimpleDriverDataSource(
                    driver, MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(Arrays.stream(classpathLocations)
                    .map(ClassPathResource::new)
                    .toArray(ClassPathResource[]::new));
            populator.execute(dataSource);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            INITIALIZED_SCHEMAS.remove(schemaKey);
            throw new IllegalStateException("Failed to initialize the test schema", exception);
        }
    }

    private static void ensureMySqlRunning() {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
    }

    private static void ensureRedisRunning() {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }

    private static String utcJdbcUrl(String jdbcUrl) {
        String separator = jdbcUrl.contains("?") ? "&" : "?";
        return jdbcUrl + separator
                + "connectionTimeZone=%2B00:00&forceConnectionTimeZoneToSession=true&preserveInstants=true";
    }

    /**
     * 共享 MySQL 的连接信息快照。
     */
    @Data
    public static final class MySqlConnectionDetails {
        private final String jdbcUrl;
        private final String username;
        private final String password;
        private final String driverClassName;
    }

    /**
     * 共享 Redis 的连接信息快照。
     */
    @Data
    public static final class RedisConnectionDetails {
        private final String host;
        private final int port;
        private final int database;
    }
}
