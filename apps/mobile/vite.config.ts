import process from 'node:process';
import { fileURLToPath, URL } from 'node:url';

import tailwindcss from '@tailwindcss/vite';
import { VantResolver } from '@vant/auto-import-resolver';
import vue from '@vitejs/plugin-vue';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    base: env.VITE_APP_BASE || '/',
    plugins: [
      tailwindcss(),
      vue(),
      AutoImport({
        dts: 'auto-imports.d.ts',
        resolvers: [VantResolver()],
      }),
      Components({
        dts: 'components.d.ts',
        resolvers: [VantResolver()],
      }),
    ],
    resolve: {
      alias: {
        '#': fileURLToPath(new URL('src', import.meta.url)),
      },
    },
    server: {
      port: Number(env.VITE_APP_PORT || 5078),
      proxy: {
        '/api': {
          changeOrigin: true,
          target: 'http://localhost:3888',
        },
      },
    },
    test: {
      environment: 'happy-dom',
      server: {
        deps: {
          inline: ['vant'],
        },
      },
      setupFiles: ['./src/test/setup.ts'],
    },
  };
});
