import type { App } from 'vue';

import { watchEffect } from 'vue';
import { createRouter } from 'vue-router';

import { useTitle } from '@vueuse/core';

import { $t } from '#/locales';

import { createAppRouterHistory } from './history';
import { routes } from './routes';

const appTitle = import.meta.env.VITE_APP_TITLE;

export const router = createRouter({
  history: createAppRouterHistory(),
  routes,
  scrollBehavior: () => ({ left: 0, top: 0 }),
});

export async function setupRouter(app: App) {
  app.use(router);
  await router.isReady();
  watchEffect(() => {
    const routeTitle = router.currentRoute.value.meta?.title;
    const pageTitle = (routeTitle ? `${$t(routeTitle)} - ` : '') + appTitle;
    useTitle(pageTitle);
  });
}
