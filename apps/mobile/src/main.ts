import './styles/base.css';

async function initApplication() {
  const { bootstrap } = await import('./bootstrap');
  await bootstrap();
}

initApplication();
