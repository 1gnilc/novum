import { initPreferences } from '@vben/preferences';

import { overridesPreferences } from './preferences';

import './styles/base.css';

async function initApplication() {
  const namespace = import.meta.env.VITE_APP_NAMESPACE || 'novum-mobile';
  await initPreferences({ namespace, overrides: overridesPreferences });
  const { bootstrap } = await import('./bootstrap');
  await bootstrap(namespace);
}

initApplication();
