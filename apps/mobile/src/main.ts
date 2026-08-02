import { loadLocale } from './locales';

import './styles/base.css';

async function initApplication() {
  const locale = await loadLocale();
  const { bootstrap } = await import('./bootstrap');
  await bootstrap(locale);
}

initApplication();
