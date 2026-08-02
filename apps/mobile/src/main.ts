import './styles/base.css';

async function initApplication() {
  const namespace = import.meta.env.VITE_APP_NAMESPACE || 'novum-mobile';
  const { bootstrap } = await import('./bootstrap');
  await bootstrap(namespace);
}

initApplication();
