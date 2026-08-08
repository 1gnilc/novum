# 上下文索引

[English](CONTEXT.md)

Novum 包含三个相关的领域上下文。只阅读与所修改行为相关的上下文；当工作跨越认证或授权与应用边界时，同时阅读两侧上下文。

## 上下文

- [Server](apps/server/CONTEXT_zh-CN.md)：建立身份、评估受保护访问并实施后端 API。
- [Admin System](apps/admin/CONTEXT_zh-CN.md)：管理管理员账户、RBAC 资源、导航和动态国际化消息。
- [Mobile Application](apps/mobile/CONTEXT_zh-CN.md)：独立于管理系统为 Customer 提供服务。

## 代码范围

- `apps/server/gnilc-auth/gnilc-auth-core/**` 主要属于 Server 认证与授权。
- `apps/admin/**` 主要属于 Admin System。
- `apps/mobile/**` 主要属于 Mobile Application。
- `apps/server/novum-core/**` 和 `apps/server/gnilc-auth/gnilc-auth-rbac/**` 同时服务于两个上下文：实施行为阅读 Server，管理行为阅读 Admin System。
- `apps/server/deploy/sql/**` 可能初始化任一上下文；按被初始化的资源判断。

## 关系

- **Admin System -> Server**：管理员凭据建立访问身份；角色和权限提供授权事实。
- **Server -> Admin System**：Server 返回认证或授权结果，但不重新定义管理员、菜单或管理术语。
- **Mobile Application 与 Admin System**：Customer 和 Admin User 是不同身份，不共享应用会话契约。
- **动态国际化 -> 业务资源**：消息提供可选展示文本，绝不拥有引用其 Message Key 的菜单或其他资源。

## 决策

- 所有架构决策都位于根目录 [`docs/adr/`](docs/adr/)，包括只适用于单个上下文的决策。
