import { createApp } from 'vue';

import App from './app.vue';
import { setupI18n } from './locales';
import { setupRouter } from './router';
import { initStores } from './stores';

export async function bootstrap() {
  const app = createApp(App);

  await setupI18n(app);
  await initStores(app);
  await setupRouter(app);
  app.mount('#app');
}
