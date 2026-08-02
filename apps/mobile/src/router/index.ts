import type { App } from 'vue';

import { computed } from 'vue';
import { createRouter, createWebHashHistory } from 'vue-router';

import { useTitle } from '@vueuse/core';

import { $t } from '#/locales';

import { routes } from './routes';

const appTitle = import.meta.env.VITE_APP_TITLE || 'Novum Mobile';

export const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: () => ({ left: 0, top: 0 }),
});

export async function setupRouter(app: App) {
  app.use(router);
  await router.isReady();
  useTitle(
    computed(() => {
      const key = router.currentRoute.value.meta.title;
      return key ? `${$t(key)} - ${appTitle}` : appTitle;
    }),
  );
}
