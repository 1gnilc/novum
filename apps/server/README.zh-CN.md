# Novum

[English](README.md)

Novum 是一个面向 Java/Spring 应用的认证与授权框架，提供 RBAC 访问控制能力。项目采用 Maven 多模块结构，提供核心授权抽象、可选 Servlet 认证支持以及面向 RBAC 的实现。

Java 包按职责区分：`com.gnilc.auth.authn.*` 表示认证，`com.gnilc.auth.authz.*` 表示授权，`com.gnilc.novum.*` 表示编排认证、授权和 RBAC 资源的系统后台管理模块。`com.gnilc.novum.auth.*` 放置后台管理员会话认证和系统访问拒绝响应等 auth adapter。

## 模块

- `gnilc-common`：common 模块的 parent/aggregator。
- `gnilc-common/gnilc-common-core`（`gnilc-common-core`）：共享响应、分页、Bean 拷贝、前置条件和异常工具。
- `gnilc-common/gnilc-test-support`（`gnilc-test-support`）：无业务语义的共享测试容器、清理和测试工具。
- `gnilc-auth`：认证与授权模块的 parent/aggregator。
- `gnilc-auth/gnilc-auth-core`（`gnilc-auth-core`）：访问控制核心注解、决策、权限提供者以及可选 Servlet 认证/授权 adapter。
- `gnilc-auth/gnilc-auth-rbac`（`gnilc-auth-rbac`）：RBAC 实体、Mapper、服务、Controller、权限提供者和缓存行为。
- `novum-core`：后台管理员资料、会话和系统级 auth 编排。
- `novum-bootstrap`：可执行应用和全应用集成边界。

## 授权核心

`authz` 由两个功能模块组成：授权和权限校验。权限校验从 `AccessDecision` 开始；它只判断已授予权限是否满足所需权限。授权负责围绕本次决策准备访问事实和权限集合。

第一层包含授权与权限校验核心 module：`AccessDecision`、`GrantedPermissionsProvider`、`RequiredPermissionsProvider`、`AccessContext`（`AccessEnvironment`、`AccessIdentity`、`AccessTarget`）、`Permission`、`AccessDenied` 和 `AccessDeniedHandler`。`AccessDenied` 是决策后的全局访问拒绝入口，`AccessDeniedHandler` 是默认 implementation 可使用的有序策略；二者都不参与权限校验。

第二层包含 adapter/helper seam：`AccessContextAdapter`、`AccessEnvironmentResolver`、`AccessIdentityResolver` 和 `AccessTargetResolver`。`AccessContextAdapter` 是执行环境进入 authz 的主 seam；环境、身份和目标 resolver 是 adapter 内部可组合的 helper seam，不是强依赖。

第三层包含两个互不强依赖的功能模块：负责准备访问事实并调用 `AccessDecision` 的环境入口 implementation，以及把 `AccessContext` 映射为权限集合的 concrete `GrantedPermissionsProvider` / `RequiredPermissionsProvider` implementation。它们依赖核心 interface，而不依赖彼此的 implementation。

`AccessDenied` 通过 `denied(AccessContext, AccessDeniedContext)` 执行 `AccessDecision` 返回 false 后的访问拒绝。`AccessDeniedContext` 承载执行环境拒绝数据，与授权事实 `AccessContext` 分离。默认 implementation 会收集有序的 `AccessDeniedHandler` 策略，并调用所有 `supports(context, deniedContext)` 返回 true 的 handler。没有支持者时按 no-op 处理。

## 可选 Servlet 认证

应用可以通过定义一个或多个 `ServletAuthenticationHandler` Bean 启用认证过滤器。没有 handler bean 时，认证过滤器不会注册。

多个 handler 按 Spring order 排序。不支持当前请求的 handler 会被跳过；如果没有任何 handler 支持当前请求，请求链继续执行，由授权规则决定匿名访问是否允许。认证失败会停止请求链，默认返回 401。授权失败仍保持独立，继续由现有访问拒绝处理路径负责。

## 响应体业务码

`R.code` 与 HTTP Status 相互独立，并且 `R` 本身不限制 code 的取值范围。HTTP 状态应从 transport response 读取，应用结果应从 JSON 响应体读取。内置 `ResponseCode` 当前约定：`0` 表示成功，`10000-19999` 表示通用业务/请求错误，`20000-29999` 表示认证、会话和授权错误。

## 环境要求

- JDK 17+
- Maven 3.8+

## 构建与测试

在仓库根目录使用以下跨平台 pnpm 命令：

```bash
pnpm dev:server
pnpm build:server
pnpm test:server
pnpm verify:server
```

Node runner `scripts/maven.mjs` 会优先在 macOS/Linux 使用 `mvnw`，在 Windows 使用 `mvnw.cmd`；如果项目没有 Maven Wrapper，则回退到 `PATH` 中的 `mvn`/`mvn.cmd`。

也可以在当前目录直接运行 Maven：

```bash
# Surefire 快速测试：*Test 和 *ControllerTest
mvn test

# 完整验证：Surefire 加 Failsafe *IT/*MapperIT/*CacheIT/*ApiIT
# Testcontainers MySQL 8 和 Redis 8 需要 Docker。
mvn verify

# 验证后构建整个 reactor
mvn clean package
```

## 分环境 S3 配置

S3 配置直接放在 `application-dev.yml` 和 `application-prod.yml` 的 `app.s3` 下，基础 `application.yml` 不包含 S3 配置。不再需要环境变量加载脚本或 IDEA 环境文件配置。使用 `pnpm dev:server` 正常启动，或者在 IntelliJ IDEA 中直接运行或调试 `NovumBootApplication` 即可。

`application-dev.yml` 中的本地开发凭据仅用于当前工作环境，并通过 Git 的 `skip-worktree` 标记排除在交付内容之外。生产环境默认关闭 S3，必须配置独立的 endpoint、bucket、凭据和 public base URL 后才能启用；禁止把开发凭据复制到生产配置。

所有测试都放在所属模块的 `src/test` 下，不使用 `src/intg-test` source set。集成测试只使用一次性的 Testcontainers 服务，不使用 H2、本地服务或共享服务。测试命名、模块选择、数据清理和 HTTP 断言规则见必须遵循的[测试指南](../../docs/test/testing-guide.md)。

## 提交规范

提交信息应遵循项目规范：[.github/commit-convention.md](../../.github/commit-convention.md)。
