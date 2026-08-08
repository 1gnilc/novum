# Repository Agent Instructions

## Instruction Scope

- This file contains rules shared by the whole repository.
- Before changing a subtree, check for a nearer `AGENTS.md`; local instructions add to or override this file for that subtree.
- This repository uses only `AGENTS.md` for agent instructions and `CONTEXT.md` for domain context. Do not duplicate these rules in tool-specific instruction files.
- Keep one `AGENTS.md` and one `CONTEXT.md` per independently managed large module. Do not add finer-grained files unless that subtree has genuinely independent ownership and rules.
- Agent hosts that do not discover `AGENTS.md` automatically must be configured or prompted to read the applicable files.
- See [`docs/agents/instruction-files.md`](docs/agents/instruction-files.md) for discovery and precedence rules.

## Domain Documentation

- Before changing domain behavior or terminology, read the root [`CONTEXT.md`](CONTEXT.md), then the relevant module `CONTEXT.md` and ADRs.
- `CONTEXT.md` files are glossaries only. Put implementation constraints in `AGENTS.md` and durable architectural decisions in ADRs.
- All ADRs live in the root [`docs/adr/`](docs/adr/), including decisions scoped to a single module.
- See [`docs/agents/domain.md`](docs/agents/domain.md).

## Code Design And Style

### Minimal Implementation

- Implement only confirmed current requirements. Do not add fields, APIs, classes, configuration, states, or compatibility paths for hypothetical future use.
- Do not introduce an abstraction unless it removes real duplication, reduces meaningful complexity, or enforces an established module boundary.
- Reuse an existing component, framework API, or module capability when it already owns the required behavior.
- Keep changes scoped to the requested behavior. Do not mix unrelated refactors into feature work.

### Replacement And Cleanup

- When a new implementation replaces an old one, remove the old class, method, component, route, type, configuration, and tests. Do not maintain parallel implementations without an explicit compatibility requirement.
- When renaming a concept, update its filenames, exports, references, routes, types, messages, tests, and generated declarations. Do not mix old and new terminology.
- Remove temporary scripts, diagnostic code, transition configuration, commented-out code, and unused implementations before completing the task.
- Do not keep aliases, dual-format readers, or legacy entry points unless a current compatibility requirement explicitly needs them.
- Search for all references after deleting or renaming code and verify that no stale path remains.

### Single Source Of Truth

- Give each concept one canonical name, one authoritative type or definition, one configuration source, and one primary execution path.
- Centralize constants, enums, defaults, supported-value collections, and shared rules at their owning boundary; import them elsewhere instead of redeclaring them.
- Store state at the source closest to the fact. Do not mirror the same state across components, stores, routes, caches, or persistence without a synchronization requirement.
- Do not duplicate configuration across source code, environment files, launch scripts, IDE settings, and documentation.
- Derive presentation data at the appropriate output boundary instead of persisting it beside its canonical source value.

### Framework And API Usage

- Use the framework's supported lifecycle, component contract, and extension points instead of manually rebuilding an existing workflow.
- Replace deprecated APIs with their supported alternatives. Do not suppress the warning, wrap the deprecated API, or use a type assertion to keep the obsolete call pattern.
- Fix a deprecated workflow at its owning boundary, not only at the line where the warning appears.
- When wrapping a third-party component, preserve all props, attributes, events, and slots that the wrapper does not explicitly override.
- A wrapper may change only the behavior it deliberately owns and must not accidentally narrow the underlying component's capabilities.

### Types

- Declare types at component, composable, form, service, or API initialization boundaries and let calls infer those types.
- Do not repeatedly override return types with call-site generics when the owning API can be typed once.
- Avoid `any`, double assertions, unjustified non-null assertions, and other type escapes that hide model defects.
- Prefer inference, constrained generics, discriminated unions, and standard utility types to duplicated type declarations.
- Group API types by cohesive resource and derive operation inputs with utilities such as `Pick`, `Omit`, and `Partial` instead of creating near-identical interfaces for every operation.
- Type names must express domain meaning rather than repeat information already evident from the underlying language type.

### Naming

- Name classes, methods, variables, and components after their responsibility or behavior, not an incidental implementation detail.
- Use clear verbs for operations, such as `validate`, `create`, `remove`, and `resolve`.
- Avoid redundant type words in names when the parameter or return type already communicates them.
- Prefer stable domain or protocol terms over vendor, framework, or infrastructure brand names in project-owned public interfaces.
- Use abbreviations only when the repository already uses the same canonical form.

### Module Boundaries

- Place code in the module that owns the responsibility; do not copy state or implementation across modules merely for caller convenience.
- Keep UI entry points focused on interaction and presentation. Put reusable business rules in the owning service or domain boundary.
- Keep controllers and page components thin. They should parse or present data and delegate substantive workflows.
- Generic utilities must not depend on a concrete page, route, store, or UI component.
- Collaborate across modules through explicit public interfaces rather than importing another module's internal implementation.
- Similar behavior alone does not justify a shared dependency. Extract only behavior that is stable and genuinely independent of its callers.

### Control Flow And Errors

- Prefer clear `async`/`await` control flow for asynchronous work.
- Restore loading, locking, and other temporary state with `finally` when cleanup must happen on every outcome.
- Catch only errors that the current layer can handle. Propagate errors that require a higher-level decision.
- Do not use empty catches, silent fallbacks, or default-success behavior that conceals failure.
- Use the owning framework's validation, submission, persistence, and lifecycle flow rather than manually composing duplicate steps at each caller.
- Handle transaction boundaries, idempotency, and failure ordering in the layer that owns data consistency.

### Frontend Code

- Use `<script setup lang="ts">` for Vue components and keep types, state, computed values, handlers, and component API setup organized and easy to scan.
- Use BEM for manually defined CSS classes. Sass nesting may use `&__element` and `&--modifier` to express those relationships.
- Reserve camelCase for TypeScript and JavaScript identifiers; do not use it as an alternate CSS class convention.
- Use utility classes for one-off layout or sizing only when no meaningful semantic class exists. Use semantic component styles for reusable structure, state, and responsive behavior.
- Give icon-only controls and custom interactive elements an accessible name, and provide keyboard behavior where native semantics do not supply it.
- Keep user-facing text in the owning localization resources instead of scattering hard-coded messages through components.

### Backend Code

- Use constructor injection and `private final` dependencies for Spring beans.
- Keep controllers limited to transport concerns, request parsing, service delegation, and response construction.
- Put business validation, state transitions, transactions, and external-resource coordination in the owning service.
- Bind related external configuration through typed configuration objects instead of reading scattered environment values in business code.
- Prefer framework service-level persistence APIs; descend to a mapper or custom query only when the service API cannot express the required operation.
- Make time, timezone, and serialization behavior explicit rather than relying on machine defaults.
- Return stable, client-appropriate business errors and do not expose internal exception details.

### Comments And Documentation

- Write comments only for constraints, reasons, or tradeoffs that are not evident from the code. Do not narrate straightforward statements.
- Do not preserve deleted implementations in comments.
- Put execution rules in `AGENTS.md`, domain terminology in `CONTEXT.md`, and durable architectural decisions in ADRs.
- Do not implement agent, editor, IDE, or local workflow requirements as application runtime behavior.

### Completion Criteria

- Inspect adjacent code and search for an existing capability before adding a new implementation.
- After replacing or renaming code, verify that the complete old reference chain has been removed.
- Run the owning module's formatter, static analysis, type checks, and tests in proportion to the change.
- Do not make checks pass by disabling rules, ignoring errors, or widening type escapes.
- Ensure the final diff contains no unrelated refactor, temporary artifact, generated noise, or accidental configuration rollback.

## Issue Tracker

- Issues and PRDs are tracked in GitHub Issues through `gh`. See [`docs/agents/issue-tracker.md`](docs/agents/issue-tracker.md).
- Use the canonical triage labels documented in [`docs/agents/triage-labels.md`](docs/agents/triage-labels.md).

## Testing

- Before writing, changing, reviewing, or running tests, read [`docs/test/testing-guide.md`](docs/test/testing-guide.md).
- Keep tests under the owning module's `src/test`; do not use `src/intg-test`.
- Run fast backend tests with `mvn -f apps/server/pom.xml test`.
- Run the complete backend suite with `mvn -f apps/server/pom.xml verify`. It requires Docker for Testcontainers MySQL 8 and Redis 8; do not substitute H2, local services, or shared services.

## Agent Development Ports

- When an AI agent starts Admin or Mobile in development, it must use the port currently configured in the owning application's `.env.development`: `VITE_PORT` for Admin and `VITE_APP_PORT` for Mobile. Start the normal development command so Vite loads that file; do not hard-code the current value or override it with a command-line port.
- When an AI agent starts the backend, it must use the current `server.port` value from `apps/server/novum-bootstrap/src/main/resources/application.yml`; do not hard-code or override that value in the launch command.
- Immediately before starting any of these services, an AI agent must read the current configured port, inspect that exact port, and automatically terminate any process listening on it. It must not choose a different port or modify application configuration merely to avoid a port conflict.

## Git And Delivery

- Leave changes uncommitted unless the user explicitly requests a commit.
- When a commit is requested, use `pnpm run commit`; never invoke `git commit` directly.
- Do not push or create or update a pull request unless the user explicitly requests remote delivery.
- Remote delivery must use a pull request. Never push directly to `main` or another target branch; default the PR base to `main` unless the user specifies otherwise.
