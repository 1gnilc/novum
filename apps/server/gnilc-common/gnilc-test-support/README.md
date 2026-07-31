# gnilc-test-support

`gnilc-test-support` 是面向 Spring Boot 3 项目的通用测试支持库，提供 MySQL 8、Redis 8 Testcontainers、数据清理保护、测试基线恢复和 RestAssured 随机端口测试支持。

该模块只包含无业务语义的测试基础设施。业务表结构、基础数据、登录方式和领域断言应由使用方在自己的测试代码中维护，不能放入本模块。

## 适用场景

| 测试场景 | 建议用法 |
| --- | --- |
| 纯单元测试 | 不使用容器，直接使用 JUnit 5、Mockito 和 AssertJ |
| MyBatis/数据库集成测试 | `MySqlContainerContextInitializer` 或自定义组合初始化器 |
| Redis 集成测试 | `RedisContainerContextInitializer` |
| 同时依赖 MySQL 和 Redis | `FullStackContainerContextInitializer` |
| 随机端口 API 测试 | `@ApiTest` + `ApiTestSupport` |
| API 测试的数据基线 | 由业务模块提供 `BaselineDataSeeder` Bean |

## Maven 依赖

同一多模块项目中使用：

```xml
<dependency>
    <groupId>com.gnilc.novum</groupId>
    <artifactId>gnilc-test-support</artifactId>
    <version>${project.version}</version>
    <scope>test</scope>
</dependency>
```

运行容器测试需要：

- Java 17 或更高版本；
- Spring Boot 3；
- 可用的 Docker 环境；
- Maven Failsafe 执行 `*IT`、`*MapperIT`、`*CacheIT` 和 `*ApiIT`。

默认容器镜像为 `mysql:8.4.0` 和 `redis:8-alpine`，可通过 JVM 系统属性覆盖：

```bash
mvn verify \
  -Dapp.test.mysql.image=mysql:8.4.0 \
  -Dapp.test.redis.image=redis:8-alpine
```

## 核心组件

| 组件 | 职责 |
| --- | --- |
| `SharedTestContainers` | 在测试 JVM 内共享 MySQL、Redis 容器并执行 classpath SQL |
| `MySqlContainerContextInitializer` | 注入容器 MySQL 数据源配置 |
| `RedisContainerContextInitializer` | 注入容器 Redis 配置 |
| `FullStackContainerContextInitializer` | 组合 MySQL 和 Redis 初始化器 |
| `ApiTest` | 组合随机端口、`test` profile、容器和数据重置监听器 |
| `ApiTestSupport` | 配置并复位 RestAssured 的随机端口状态 |
| `BaselineDataSeeder` | 使用方实现的业务基线扩展点 |
| `TestDataResetManager` | 编排 Redis、MySQL 清理和基线恢复 |
| `TestEnvironmentGuard` | 在破坏性清理前校验 profile、开关和容器端点 |

## 案例一：MySQL 模块集成测试

业务模块负责提供 schema SQL，并在自己的初始化器中声明执行顺序：

```java
public final class OrderTestInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        new MySqlContainerContextInitializer().initialize(context);
        SharedTestContainers.initializeMySqlSchema("sql/schema/order.sql");
    }
}
```

```java
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = OrderTestInitializer.class)
@Transactional
class OrderServiceIT {
    @Autowired
    private OrderService orderService;

    @Test
    void createOrderPersistsToMysql() {
        Long id = orderService.createOrder();

        assertThat(id).isNotNull();
    }
}
```

事务内的 Mapper、Service 集成测试优先使用 `@Transactional` 回滚。不要用 H2、本机数据库或共享数据库替代容器 MySQL。

## 案例二：Redis 集成测试

```java
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = RedisContainerContextInitializer.class)
class TokenCacheIT {
    @Autowired
    private StringRedisTemplate redis;

    @AfterEach
    void cleanRedis() {
        new RedisCleaner(redis.getConnectionFactory()).flushDatabase();
    }

    @Test
    void tokenHasExpectedValueAndTtl() {
        // 执行业务行为后断言 key、value 和 TTL。
    }
}
```

只依赖 Redis 时不要启动 MySQL。测试必须清理当前容器 Redis 数据库，避免测试方法相互污染。

## 案例三：随机端口 API 测试

业务模块提供可重复执行的基线写入器：

```java
@TestConfiguration(proxyBeanMethods = false)
class OrderApiTestConfiguration {
    @Bean
    BaselineDataSeeder orderBaselineSeeder(JdbcTemplate jdbc) {
        return () -> jdbc.update("insert into tenant (id, name) values (1, 'test')");
    }
}
```

```java
@ApiTest
@Import(OrderApiTestConfiguration.class)
class OrderApiIT extends ApiTestSupport {
    @Test
    void createOrderRunsThroughRealHttpAndMysql() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"productId\":1}")
                .when()
                .post("/api/orders")
                .then()
                .statusCode(200);
    }
}
```

`@ApiTest` 在每个测试方法前后执行以下流程：

1. 校验当前连接确实属于本测试 JVM 的容器；
2. 清空 Redis 当前数据库；
3. 截断 MySQL 当前 schema 的业务表；
4. 按 Spring 顺序执行全部 `BaselineDataSeeder`；
5. 再次清空基线写入过程中产生的缓存；
6. 测试结束后兜底清理 Redis 和 MySQL。

随机端口请求运行在服务端线程，不能依赖测试方法上的事务回滚。

## Schema 资源约定

`SharedTestContainers.initializeMySqlSchema(...)` 接收 classpath 路径，并按参数顺序执行脚本。同一组路径在一个测试 JVM 内只初始化一次：

```java
SharedTestContainers.initializeMySqlSchema(
        "sql/schema/01_domain.sql",
        "sql/schema/02_seed.sql");
```

SQL 应由使用方通过 Maven `testResources` 放入测试 classpath。例如：

```xml
<testResource>
    <directory>${maven.multiModuleProjectDirectory}/deploy/sql</directory>
    <targetPath>sql/schema</targetPath>
    <includes>
        <include>*.sql</include>
    </includes>
</testResource>
```

## 清理安全机制

`DatabaseCleaner` 会截断当前 schema 中除 Flyway、Liquibase、Quartz 和 Seata 元数据表之外的业务表；`RedisCleaner` 会执行 `FLUSHDB`。这些操作具有破坏性，因此 `TestEnvironmentGuard` 要求同时满足：

- Spring 激活 `test` profile；
- `app.test.cleanup.enabled=true`；
- 数据库名为测试库；
- JDBC 和 Redis 实际端点与当前 Testcontainers 实例完全一致。

任一条件不满足时，清理会直接失败。不要绕过该保护，也不要把清理组件用于开发、预发布或生产环境。

## 作为独立通用库的边界

后续独立发布时应继续保持以下边界：

- 不依赖任何业务模块；
- 不内置业务表名、业务 SQL、账号或角色；
- 容器镜像、数据库名和清理策略应保持可配置；
- 公共 API 变更需要保持向后兼容；
- 本模块自身使用 Docker-free 单元测试验证清理编排和安全保护；
- 真实 MySQL、Redis 行为由消费模块的集成测试验证。

## 执行命令

```bash
# Docker-free 快速测试
mvn test

# 包含 MySQL、Redis 和随机端口 API 测试
mvn verify
```

完整测试分层和仓库约束见 [`../../docs/test/test-strategy.md`](../../docs/test/test-strategy.md) 和 [`../../docs/test/testing-guide.md`](../../docs/test/testing-guide.md)。
