# Mobile 应用指令

[English](AGENTS.md)

## 范围

- 将 Mobile 构建为标准 Vue 3、TypeScript、Vite 和 Vant 应用。
- 使 Customer 术语与 `CONTEXT.md` 和根目录 ADR 保持一致。
- 文件、名称和公共接口应简洁、明确，并与仓库相邻代码保持一致。

## 边界

- Vant 是唯一的 UI 组件库。
- 路由保持静态且在客户端运行。不要增加认证路由守卫或后端路由生成。
- 使用 `meta.requiresAuth: true` 标记受保护页面。
- 全局登录提示从当前路由和认证状态派生；不要把路由状态复制到 Store。
- 不要依赖 Admin 的布局、适配器、Store、动态语言资源或 UI 包。
- Mobile 偏好设置保存在自己的有类型 Pinia Store 中，并通过现有 Mobile Pinia 持久化设置进行持久化。不要依赖 `@vben/preferences`，也不要增加 Admin 偏好字段、UI、布局或主题行为。
- Mobile 的 `vue-i18n`、Vant 和 Day.js 语言管线保持在应用内部。不要依赖 `@vben/locales`，也不要增加 Admin 动态消息。
- 只在实现计划记录的具体 Mobile 使用位置复用 `@vben/request` 和 `@vben/utils`。

## 依赖

- 只为已实现的 Mobile 使用场景添加依赖。
- 使用工作区包和根目录 catalog 版本。
- 通过已配置的解析器按需使用 Vant 组件、函数 API 和样式。

## 样式

- 每个手工定义的 CSS 类都使用 BEM 名称，包括 scoped Vue 样式块中的类。
- 当 BEM 关系能因此更清楚时，使用 `&__element` 和 `&--modifier` 等 Sass 嵌套。
- camelCase 只用于 TypeScript 和 JavaScript 标识符，不要将其作为另一种 CSS 类命名约定。
- 只有元素没有有意义的语义类名时才使用 Tailwind 工具类；可复用组件结构和状态优先使用组件样式。
