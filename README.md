# Novum Monorepo

This repository contains the Novum product as a pnpm and Turborepo monorepo based on [Vue Vben Admin](https://github.com/vbenjs/vue-vben-admin).

## Applications

- `apps/admin`: Vue 3 + Element Plus administration UI, derived from Vben's `web-ele` application.
- `apps/server`: Spring Boot 3 and Maven multi-module authentication and authorization server.

The Vben workspace packages required by the admin application remain under `packages/`, `internal/`, and `scripts/`.

## Requirements

- Node.js `^22.18.0 || ^24.0.0`
- pnpm `>=11.0.0`
- JDK 17+
- Maven 3.8+
- Docker for the server integration test suite

## Development

```bash
nvm use
pnpm install
pnpm dev:admin
pnpm dev:server
```

The admin UI runs on port `5777` and proxies `/api` requests to the server on port `3888`.

## Build and test

```bash
pnpm build
pnpm check
pnpm test
pnpm verify:server
```

See [apps/server/README.md](apps/server/README.md) for the server architecture and [docs/test/testing-guide.md](docs/test/testing-guide.md) for mandatory server test conventions.

## Repository guidance

Shared agent instructions live in `AGENTS.md`. Start domain discovery from the root `CONTEXT.md`, which links module-owned contexts and the centralized ADR collection. See [docs/agents/instruction-files.md](docs/agents/instruction-files.md).
