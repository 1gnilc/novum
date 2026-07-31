# Novum Monorepo

本仓库以 [Vue Vben Admin](https://github.com/vbenjs/vue-vben-admin) 的 pnpm + Turborepo 工具链为基础，包含两个核心应用：

- `apps/admin`：基于 Vben `web-ele` 的 Vue 3 + Element Plus 管理后台。
- `apps/server`：原有 Spring Boot 3 + Maven 多模块认证授权服务。

管理后台依赖的 Vben 工作区包保留在 `packages/`、`internal/` 和 `scripts/`。

## 环境要求

- Node.js `^22.18.0 || ^24.0.0`
- pnpm `>=11.0.0`
- JDK 17+
- Maven 3.8+
- 运行服务端集成测试时需要 Docker

## 开发

```bash
nvm use
pnpm install
pnpm dev:admin
pnpm dev:server
```

管理后台运行在 `5777` 端口，并将 `/api` 请求代理到 `3888` 端口的服务端。

## 构建和测试

```bash
pnpm build
pnpm check
pnpm test
pnpm verify:server
```

服务端说明见 [apps/server/README.zh-CN.md](apps/server/README.zh-CN.md)，强制测试规范见 [docs/test/testing-guide.md](docs/test/testing-guide.md)。

共享 Agent 指令位于根目录 `AGENTS.md`。领域文档从根目录 `CONTEXT.md` 开始，并按链接读取模块自有的上下文和根目录集中管理的 ADR；加载规则见 [docs/agents/instruction-files.md](docs/agents/instruction-files.md)。
