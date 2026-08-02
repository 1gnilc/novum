# Mobile Application Instructions

## Scope

- Build Mobile as a standard Vue 3, TypeScript, Vite, and Vant application.
- Keep Customer terminology aligned with `CONTEXT.md` and the root ADRs.
- Keep files, names, and public interfaces concise, explicit, and consistent with adjacent repository code.

## Boundaries

- Use Vant as the only UI component library.
- Keep routes static and client-side. Do not add authentication router guards or backend route generation.
- Mark protected requests with `requestAuth.required: true` and protected pages with `meta.requiresAuth: true`.
- Keep the global login prompt derived from the current route and authentication state; do not copy route state into a Store.
- Do not depend on Admin layouts, adapters, preferences, stores, dynamic locales, or UI packages.
- Reuse `@vben/request` and `@vben/utils` only at the concrete Mobile use sites documented in the implementation plan.

## Dependencies

- Add dependencies only for an implemented Mobile use case.
- Use workspace packages and root catalog versions.
- Keep Vant components, function APIs, and styles on-demand through the configured resolvers.
