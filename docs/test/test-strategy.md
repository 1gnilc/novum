# Test strategy

This repository uses one test source set per owning Maven module. Test names select the execution lane:

| Suffix | Scope | Maven lane |
| --- | --- | --- |
| `*Test`, `*ControllerTest` | Unit, focused auto-configuration, and HTTP controller contracts | Surefire, `mvn -f apps/server/pom.xml test` |
| `*MapperIT` | MyBatis-Plus mappings and MySQL behavior | Failsafe, `mvn -f apps/server/pom.xml verify` |
| `*CacheIT`, `*IT` | Spring integration and Redis behavior | Failsafe, `mvn -f apps/server/pom.xml verify` |
| `*ApiIT` | Random-port HTTP flows | Failsafe, `mvn -f apps/server/pom.xml verify` |

## Package locality

Test packages mirror the production packages they verify. A focused test for `com.gnilc.feature.cache.TokenCache` belongs in the same package under the owning module's `src/test/java`, and its class name should identify that target. Do not collect unrelated `context`, `provider`, `filter`, controller, or utility behavior in a module-level catch-all test class.

A test may cover multiple production classes when their collaboration is the behavior under test, but it stays beside the narrowest shared production boundary. Mapper integration tests stay with the owning DAO package, service integration tests stay with the implementation package, and cache transaction tests stay with the cache package.

Only these support layers intentionally do not mirror one production class:

- business-neutral reusable infrastructure lives in `gnilc-test-support`;
- module-only context initializers and test applications live in that module's `support` test package;
- deployment schema tests live beside the module that owns the schema;
- random-port HTTP flows live beside the module that owns the API;
- only final whole-application composition and startup checks live in `novum-bootstrap`.

## Infrastructure

`gnilc-test-support` contains only business-neutral test infrastructure:

- JVM-scoped MySQL 8.4 and Redis 8 containers;
- Spring context property initializers;
- guarded database and Redis cleanup;
- API baseline reset orchestration;
- RestAssured random-port setup.

The deployment scripts under `deploy/sql` are the only schema input. The owning modules copy only their required scripts to the test classpath and initialize them in the temporary MySQL database. Each owning module declares which deployment scripts and MyBatis properties its tests need; the shared support module contains no RBAC or admin schema knowledge. Module tests never use H2, a local database, or a local Redis service.

## Isolation

Mapper and service integration tests use Spring transaction rollback. Redis tests flush the isolated container database after each method. Random-port API tests cannot rely on test transactions, so `@ApiTest` performs this lifecycle:

1. verify the active `test` profile, cleanup flag, database name, actual JDBC endpoint, and actual Redis host, port, and database against the running Testcontainers instances;
2. flush Redis and truncate all business tables;
3. run application-owned `BaselineDataSeeder` beans;
4. flush Redis again;
5. execute the test;
6. flush Redis and truncate business tables after the test.

The system module owns the admin API baseline. It replays the real admin deployment seed and adds the protected-path permissions, menu, and limited-role account needed by its API test flows.

## Commands

```bash
mvn -f apps/server/pom.xml test
mvn -f apps/server/pom.xml verify
```

`mvn -f apps/server/pom.xml test` must remain Docker-free. `mvn -f apps/server/pom.xml verify` requires Docker and fails instead of substituting a different database or cache.
