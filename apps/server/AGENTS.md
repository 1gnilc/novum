# Server Instructions

## Domain

- Read [`CONTEXT.md`](CONTEXT.md) before changing authentication or authorization concepts.
- For administrator, RBAC-management, menu, or dynamic-internationalization behavior, also read [`../admin/CONTEXT.md`](../admin/CONTEXT.md) and the relevant ADRs in the root [`docs/adr/`](../../docs/adr/).
- Authentication establishes identity; authorization decides whether that identity may access a target. Do not merge those responsibilities.

## Authorization Boundaries

- Keep permission checking limited to allow/deny evaluation. Permission-source resolution and denied-response handling belong outside the decision component.
- Keep `AccessContext` limited to authorization facts. Servlet requests, responses, database connections, caches, and other execution objects belong in adapters or denied contexts.
- Treat `AccessContextAdapter` as the execution-environment boundary. Environment, identity, and target resolvers are optional helpers used to construct the context.
- Granted- and required-permission providers must opt into the current access environment so unrelated environments do not contribute permissions to one decision.
- Use `Web*` for functional Servlet entry points and configuration; use `Servlet*` for concrete types that depend on the Jakarta Servlet API.
- Keep system-specific authentication, authorization, RBAC, and administration adapters under `com.gnilc.novum.*`; do not move them into the framework-neutral auth core.

## Session Boundaries

- Keep access-token, refresh-token, pairing, revocation, cache-key, and TTL details inside the administrator-session implementation.
- Controllers and administrator profile workflows consume the session abstraction; they must not reconstruct token or Redis behavior.

## Persistence And Services

- In MyBatis-Plus service implementations, prefer service-level APIs such as `lambdaQuery()`, `lambdaUpdate()`, `save()`, and `updateById()` over direct `baseMapper` access.
- Preserve UTC instant handling and shared infrastructure composition defined by the relevant ADRs.

## API Contracts

- `R.code` is a JSON business code, not an HTTP status. Express HTTP status only through the transport layer, such as `ResponseEntity` or `HttpServletResponse`.
- Resolve client-correctable backend messages at their owning source; do not expose internal exception details to clients.
