# Admin 应用指令

[English](AGENTS.md)

## 领域

- 修改管理员、RBAC、菜单或动态国际化行为前，阅读 [`CONTEXT.md`](CONTEXT.md) 和根目录 [`docs/adr/`](../../docs/adr/) 中的相关 ADR。
- Server 拥有授权和业务不变量。UI 访问代码检查只控制操作可见性。

## API 模块

- 定义内聚的资源类型，并使用 `Pick`、`Omit` 等 TypeScript 工具在操作位置派生专用输入。
- 不要为每个 API 操作创建一个接口。
- 遵循 [Vben Admin 系统 API 模块](https://github.com/vbenjs/vue-vben-admin/tree/main/playground/src/api/system)的组织方式。

## UI 修改

- 引入新模式前，检查相邻视图和共享组件。
- 为可变抽屉和表单保留未保存更改保护。
- 按相关 ADR 的定义，使 Message Key 持久化与外围业务资源保存保持分离。
