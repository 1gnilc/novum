# Server 指令

[English](AGENTS.md)

## 领域

- 修改认证或授权概念前，阅读 [`CONTEXT.md`](CONTEXT.md)。
- 对于管理员、RBAC 管理、菜单或动态国际化行为，还要阅读 [`../admin/CONTEXT.md`](../admin/CONTEXT.md) 和根目录 [`docs/adr/`](../../docs/adr/) 中的相关 ADR。
- 认证建立身份；授权决定该身份能否访问目标。不要合并这两项职责。

## 授权边界

- 权限检查只负责允许/拒绝评估。权限来源解析和拒绝响应处理属于决策组件之外。
- `AccessContext` 只包含授权事实。Servlet 请求、响应、数据库连接、缓存和其他执行对象应放在适配器或拒绝上下文中。
- 将 `AccessContextAdapter` 视为执行环境边界。环境、身份和目标解析器是用于构造上下文的可选辅助组件。
- 已授予权限和所需权限的 Provider 必须显式选择当前访问环境，防止无关环境向同一个决策提供权限。
- 功能性 Servlet 入口和配置使用 `Web*`；依赖 Jakarta Servlet API 的具体类型使用 `Servlet*`。
- 系统专用认证、授权、RBAC 和管理适配器保留在 `com.gnilc.novum.*` 下；不要将它们移入框架中立的 auth core。

## 会话边界

- 访问令牌、刷新令牌、配对、撤销、缓存键和 TTL 细节保留在共享会话引擎及其领域专用会话管理器中。
- Admin 和 Customer 控制器使用各自的会话管理器；不得重建令牌或 Redis 行为，也不得接受另一身份领域的令牌。

## 持久化与服务

- 在 MyBatis-Plus 服务实现中，优先使用 `lambdaQuery()`、`lambdaUpdate()`、`save()` 和 `updateById()` 等服务层 API，不要直接访问 `baseMapper`。
- 保留相关 ADR 定义的 UTC 时间点处理和共享基础设施组合方式。

## API 契约

- `R.code` 是 JSON 业务码，不是 HTTP 状态。HTTP 状态只能通过 `ResponseEntity` 或 `HttpServletResponse` 等传输层表达。
- 在客户端可纠正消息的所属来源解析消息；不要向客户端暴露内部异常详情。
