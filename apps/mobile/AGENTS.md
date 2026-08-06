# Mobile Application Instructions

## Scope

- Build Mobile as a standard Vue 3, TypeScript, Vite, and Vant application.
- Keep Customer terminology aligned with `CONTEXT.md` and the root ADRs.
- Keep files, names, and public interfaces concise, explicit, and consistent with adjacent repository code.

## Boundaries

- Use Vant as the only UI component library.
- Keep routes static and client-side. Do not add authentication router guards or backend route generation.
- Mark protected pages with `meta.requiresAuth: true`.
- Keep the global login prompt derived from the current route and authentication state; do not copy route state into a Store.
- Do not depend on Admin layouts, adapters, stores, dynamic locales, or UI packages.
- Keep Mobile preferences in its own typed Pinia store and persist them through the existing Mobile Pinia persistence setup. Do not depend on `@vben/preferences` or add Admin preference fields, UI, layout, or theme behavior.
- Keep the Mobile `vue-i18n`, Vant, and Day.js locale pipeline local to the application. Do not depend on `@vben/locales` or add Admin dynamic messages.
- Reuse `@vben/request` and `@vben/utils` only at the concrete Mobile use sites documented in the implementation plan.

## Dependencies

- Add dependencies only for an implemented Mobile use case.
- Use workspace packages and root catalog versions.
- Keep Vant components, function APIs, and styles on-demand through the configured resolvers.

## Styling

- Use BEM names for every manually defined CSS class, including classes in scoped Vue style blocks.
- Use Sass nesting such as `&__element` and `&--modifier` when it makes the BEM relationship clearer.
- Reserve camelCase for TypeScript and JavaScript identifiers; do not use it as an alternate CSS class convention.
- Use Tailwind utilities only when the element has no meaningful semantic class name; prefer component styles for reusable component structure and states.
