# 部署 SQL

本目录是当前版本数据库结构和基础数据的部署入口。应用启动时不会自动执行这些脚本；生产或本地首次部署需要由运维人员按顺序执行。集成测试会将同一组脚本加载到 Testcontainers 创建的临时 MySQL 8 数据库，以校验部署脚本与代码的一致性。

## 脚本说明

### `01_rbac.sql`

创建 RBAC 模块的当前表结构：

- `az_role`
- `az_permission`
- `az_menu`
- `az_user`
- `az_user_role`
- `az_role_permission`
- `az_role_menu`

该脚本使用 `CREATE TABLE IF NOT EXISTS`，可以重复执行。对于已存在的表，脚本不会修改字段、索引或约束，因此不能替代数据库迁移。

### `02_admin.sql`

创建并初始化系统管理模块：

- 创建 `sys_admin` 表；
- 创建内置 `admin` 角色；
- 创建默认 RBAC 用户和管理员账号；
- 创建默认 `Dashboard` 和隐藏的 `Profile` 菜单；
- 建立默认管理员与 `admin` 角色的绑定关系。

该脚本依赖 `01_rbac.sql`，可以重复执行。它不会覆盖已有默认管理员资料，并会确保默认管理员、RBAC 用户、内置角色及三者的有效关系存在；如果这些默认记录被逻辑删除，脚本会恢复其 `del` 状态，并恢复默认管理员的启用状态。它不会覆盖已有记录的密码或其他资料字段，也不能替代数据库迁移。

默认管理员凭据：

- 用户名：`admin`
- 密码：`123456`

首次登录后必须立即修改默认密码。不要在公网环境中保留默认凭据。

### `03_framework_permissions.sql`

初始化 Spring Boot 框架默认端点权限。当前包含 `*:/error`；日志中两个 `/error` handler 共享同一个 RequestMapping，因此只初始化一条权限记录。

### `04_rbac_permissions.sql`

初始化 RBAC 模块的 20 条 RequestMapping 权限。

### `05_admin_permissions.sql`

初始化系统后台管理员模块的 14 条 RequestMapping 权限。当前用户资料、角色码、按钮访问码、导航路由树、基本信息修改和密码修改共 6 项权限为非公开权限，并绑定到内置 `admin` 角色；其余登录传输和后台管理员管理权限保持原有公开策略。

三个权限脚本均依赖 `01_rbac.sql`。初始化记录的 `code` 和 `name` 为 `<HTTP method>:<path>`，`target_identifier` 为请求路径，`target_qualifier` 为 HTTP method。脚本按 `code` 判断是否已存在；`05_admin_permissions.sql` 会将上述 6 项当前用户权限规范化为非公开并幂等补齐 `admin` 角色绑定，其他既有权限记录保持原值。

### `06_i18n.sql`

创建 `sys_i18n` 动态国际化消息记录表，以全局 Message Key 标识消息并使用 `default`、`admin` 分类限定语言包范围；迁移默认菜单标题 key，初始化中英文菜单翻译、`System` / `I18nMessage` 两级菜单、内置 `i18n:manager` 角色和国际化消息接口权限。bundle 权限绑定内置 `admin` 基线角色，查询与维护权限只绑定 `i18n:manager`，默认 `admin` 账号额外获得该角色。重复执行不会覆盖已存在的翻译值。

### `07_rbac_admin.sql`

初始化后台管理员、角色、权限和菜单管理能力：

- 为权限和菜单补齐固有的 `built_in` 属性；
- 创建内置 `rbac:manager` 角色，并将 RBAC 与后台管理员管理接口统一收紧为非公开；
- 初始化四个管理页面、15 个按钮菜单及中英文动态菜单标题；
- 将管理权限和菜单绑定 `rbac:manager`，并为默认 `admin` 账号授予该角色；
- 将框架、后台管理员、RBAC、国际化端点权限及系统基础菜单标记为内置资源。

该脚本依赖 `01_rbac.sql` 至 `06_i18n.sql`。它可以在当前空库结构上重复执行，也可以幂等补齐上一版 `az_permission` 和 `az_menu` 缺少的 `built_in` 字段；它不是通用迁移框架，不处理其他历史结构差异。重复执行不会覆盖已有动态翻译值。

## 首次部署

准备一个空的 MySQL 8 数据库，并按以下顺序执行：

```bash
mysql --host=<host> --user=<user> --password --database=<database> \
  < deploy/sql/01_rbac.sql
mysql --host=<host> --user=<user> --password --database=<database> \
  < deploy/sql/02_admin.sql
mysql --host=<host> --user=<user> --password --database=<database> \
  < deploy/sql/03_framework_permissions.sql
mysql --host=<host> --user=<user> --password --database=<database> \
  < deploy/sql/04_rbac_permissions.sql
mysql --host=<host> --user=<user> --password --database=<database> \
  < deploy/sql/05_admin_permissions.sql
mysql --host=<host> --user=<user> --password --database=<database> \
  < deploy/sql/06_i18n.sql
mysql --host=<host> --user=<user> --password --database=<database> \
  < deploy/sql/07_rbac_admin.sql
```

执行前确认目标数据库：

```sql
SELECT DATABASE();
```

执行完成后至少确认以下对象存在：

```sql
SHOW TABLES;
SELECT id, username, status FROM sys_admin WHERE username = 'admin' AND del = 0;
SELECT id, code, built_in FROM az_role WHERE code = 'admin' AND del = 0;
```

## 已有数据库升级

本目录主要维护当前版本的空库初始化脚本，不提供通用的历史版本迁移链。`07_rbac_admin.sql` 仅显式支持从上一版当前结构补齐权限和菜单的 `built_in` 字段；其他已有数据库升级仍不能把重复执行 `01_rbac.sql` 当作升级方案，应根据实际版本差异编写并审核迁移脚本，通过项目采用的迁移系统单独执行。

## 幂等性边界

所有脚本都支持在当前版本结构上重复执行，但幂等只表示重复执行不会重复建表或重复插入默认数据：

- `01_rbac.sql` 不会升级已有表结构；
- `02_admin.sql` 不会覆盖已有管理员资料或密码，但会恢复默认记录的逻辑删除状态和默认管理员的启用状态；
- `03_framework_permissions.sql` 和 `04_rbac_permissions.sql` 不会覆盖已有权限记录；
- `05_admin_permissions.sql` 会规范化 6 项当前用户权限的公开状态并补齐有效 `admin` 角色绑定，其他已有权限字段保持不变；
- `06_i18n.sql` 会恢复国际化内置角色、菜单和默认关系，但不会覆盖已有翻译值；
- `07_rbac_admin.sql` 会补齐 `built_in` 字段、恢复 RBAC 管理内置资源与关系、收紧管理接口公开状态，但不会覆盖已有动态翻译值；
- 脚本不会删除额外的业务数据；
- 历史版本升级、字段变更和索引变更仍需专门的迁移脚本。

## 自动化测试

使用部署脚本的模块会在各自 `pom.xml` 中将所需 SQL 复制到测试 classpath：

```text
deploy/sql/<script>.sql -> sql/schema/<script>.sql
```

当前测试加载方式如下：

- `gnilc-auth-rbac` 的 `RbacSchemaIT` 验证 `01_rbac.sql`、`03_framework_permissions.sql` 和 `04_rbac_permissions.sql`，其他模块集成测试只加载 `01_rbac.sql`；
- `novum-core` 的 Schema 集成测试验证 `02_admin.sql`、`05_admin_permissions.sql`、`06_i18n.sql` 和 `07_rbac_admin.sql`，包括上一版结构补列与重复执行；其他模块集成测试依次加载 `01_rbac.sql`、`02_admin.sql`；
- `novum-core` 的 Admin API 测试恢复基线数据时会重新执行 `02_admin.sql` 至 `07_rbac_admin.sql`，确保全库清理后框架、RBAC、后台管理员和国际化权限均恢复到真实部署基线；
- `novum-bootstrap` 只验证最终应用组合和启动，不再复制或执行部署 SQL。

测试数据库固定为 Testcontainers 创建的 `gnilc_auth_test`，测试不会使用 H2、本机 MySQL、开发数据库或共享数据库。

```bash
# Docker-free 快速测试，不执行部署 SQL
mvn test

# 完整测试，使用 MySQL 8 和 Redis 8 Testcontainers
mvn verify
```

## 安全要求

- 不要把数据库密码写入 SQL、文档、脚本参数或提交记录；使用客户端交互式密码输入或安全的凭据管理方式。
- 当前用户相关的 6 项权限默认要求 `admin` 角色；后台管理员、角色、权限和菜单管理接口要求 `rbac:manager` 角色；只有登录、刷新和退出等会话端点保持公开访问。
- 不要在共享数据库或未备份的现有数据库上试运行初始化脚本。
- 自动化测试只能连接由 Testcontainers 管理的临时数据库。
- 本目录只处理 MySQL 结构和基础数据，不负责 Redis 初始化。
