# Novum

[中文文档](README.zh-CN.md)

Novum is a Java/Spring authentication and authorization framework with RBAC-based access control support. It is organized as a multi-module Maven project and provides core authorization abstractions, optional Servlet authentication support, and an RBAC-oriented implementation.

Packages remain purpose-specific: `com.gnilc.auth.authn.*` is authentication, `com.gnilc.auth.authz.*` is authorization, and `com.gnilc.novum.*` is the system administration module that coordinates authentication, authorization, and RBAC resources. `com.gnilc.novum.auth.*` contains the system-administration auth adapters for admin-session authentication and system access-denied responses.

## Modules

- `gnilc-common`: parent/aggregator for common modules.
- `gnilc-common/gnilc-common-core` (`gnilc-common-core`): shared response, paging, bean-copy, validation, and exception utilities.
- `gnilc-common/gnilc-test-support` (`gnilc-test-support`): behavior-neutral shared test containers, cleanup, and test utilities.
- `gnilc-auth`: parent/aggregator for authentication and authorization modules.
- `gnilc-auth/gnilc-auth-core` (`gnilc-auth-core`): core access-control annotations, decisions, permission providers, and optional Servlet authentication/authorization adapters.
- `gnilc-auth/gnilc-auth-rbac` (`gnilc-auth-rbac`): RBAC entities, mappers, services, controllers, permission providers, and cache behavior.
- `novum-core`: administrator profiles, sessions, and system-level auth composition.
- `novum-bootstrap`: executable application and whole-application integration boundary.

## Authorization core

`authz` is composed of two functional modules: authorization and permission checking. Permission checking starts at `AccessDecision`; it only decides whether granted permissions satisfy required permissions. Authorization prepares the access facts and permission sets around that decision.

The first layer contains the core authz modules: `AccessDecision`, `GrantedPermissionsProvider`, `RequiredPermissionsProvider`, `AccessContext` (`AccessEnvironment`, `AccessIdentity`, `AccessTarget`), `Permission`, `AccessDenied`, and `AccessDeniedHandler`. `AccessDenied` is the global post-decision denied entry, and `AccessDeniedHandler` is an ordered strategy used by the default implementation; neither participates in permission checking.

The second layer contains adapter/helper seams: `AccessContextAdapter`, `AccessEnvironmentResolver`, `AccessIdentityResolver`, and `AccessTargetResolver`. `AccessContextAdapter` is the main seam from an execution environment into authz. The environment, identity, and target resolvers are optional helper seams composed inside an adapter; they are not strong dependencies.

The third layer contains two independent functional modules: environment-entry implementations that prepare access facts and invoke `AccessDecision`, and concrete `GrantedPermissionsProvider` / `RequiredPermissionsProvider` implementations that map an `AccessContext` to permissions. They depend on the core interfaces, not on each other's implementations.

`AccessDenied` executes the denied path after `AccessDecision` returns false through `denied(AccessContext, AccessDeniedContext)`. `AccessDeniedContext` carries execution-environment denial data separately from authorization facts. The default implementation collects ordered `AccessDeniedHandler` strategies and invokes every handler whose `supports(context, deniedContext)` returns true. If no handler supports the denied context, the default implementation is a no-op.

## Optional Servlet authentication

Applications can opt in to the authentication filter by defining one or more `ServletAuthenticationHandler` beans. If no handler bean exists, the authentication filter is not registered.

Multiple handlers are ordered by Spring order. A handler that does not support the current request is skipped; if no handler supports the request, the filter chain continues and authorization decides whether anonymous access is allowed. A failed authentication stops the chain and returns 401 by default. Authorization failures remain separate and are handled by the existing access-denied path.

## Response body codes

`R.code` is independent from the HTTP status code and does not enforce a value range. Read HTTP status from the transport response and the application result from the JSON body. The built-in `ResponseCode` convention currently uses `0` for success, `10000-19999` for common business/request failures, and `20000-29999` for authentication, session, and authorization failures.

## Requirements

- JDK 17+
- Maven 3.8+

## Build and test

From the repository root, use the cross-platform pnpm scripts:

```bash
pnpm dev:server
pnpm build:server
pnpm test:server
pnpm verify:server
```

The Node runner in `scripts/maven.mjs` selects `mvnw` on macOS/Linux or `mvnw.cmd` on Windows when wrapper files are present. Otherwise, it falls back to `mvn`/`mvn.cmd` from `PATH`.

Maven can also be invoked directly from this directory:

```bash
# Fast Surefire tests: *Test and *ControllerTest
mvn test

# Full verification: Surefire plus Failsafe *IT/*MapperIT/*CacheIT/*ApiIT
# Docker is required for Testcontainers MySQL 8 and Redis 8.
mvn verify

# Build the reactor after verification
mvn clean package
```

## Profile-specific S3 configuration

S3 values are configured directly under `app.s3` in `application-dev.yml` and `application-prod.yml`; the base `application.yml` does not contain S3 settings. No environment loader or IDEA environment-file configuration is required. Start the backend normally with `pnpm dev:server`, or run/debug `NovumBootApplication` directly in IntelliJ IDEA.

Local development credentials in `application-dev.yml` are workstation-specific and excluded from delivery with Git's `skip-worktree` flag. Production S3 stays disabled until its own endpoint, bucket, credentials, and public base URL are configured; never copy development credentials into the production profile.

All tests live under the owning module's `src/test`; there is no `src/intg-test` source set. Integration tests use disposable Testcontainers services, never H2, local services, or shared services. See the mandatory [testing guide](../../docs/test/testing-guide.md) for naming, module selection, cleanup, and HTTP assertion policy.

## Commit convention

Commit messages should follow the project convention in [.github/commit-convention.md](../../.github/commit-convention.md).
