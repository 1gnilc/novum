import type { AppLocale } from './locales';

import { createApp } from 'vue';

import App from './app.vue';
import { setupI18n } from './locales';
import { router } from './router';
import { initStores } from './stores';

export async function bootstrap(locale: AppLocale) {
  const app = createApp(App);

  app.use(setupI18n(locale));
  await initStores(app);
  app.use(router);
  await router.isReady();
  app.mount('#app');
}
