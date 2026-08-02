import type { App } from 'vue';

import { createPinia } from 'pinia';
import SecureLS from 'secure-ls';

type SecureStorage = {
  get(key: string): unknown;
  set(key: string, value: unknown): void;
};

type SecureStorageConstructor = new (config?: {
  encodingType?: string;
  encryptionSecret?: string;
  isCompression?: boolean;
  metaKey?: string;
}) => SecureStorage;

const secureStorageModule = SecureLS as unknown as {
  default?: SecureStorageConstructor;
  SecureLS?: SecureStorageConstructor;
};
const SecureStorage =
  secureStorageModule.default ??
  secureStorageModule.SecureLS ??
  (SecureLS as unknown as SecureStorageConstructor);

export async function initStores(app: App) {
  const { createPersistedState } = await import('pinia-plugin-persistedstate');
  const pinia = createPinia();
  const namespace = import.meta.env.VITE_APP_NAMESPACE || 'novum-mobile';
  const secureStorage = new SecureStorage({
    encodingType: 'aes',
    encryptionSecret: import.meta.env.VITE_APP_STORE_SECURE_KEY,
    isCompression: true,
    metaKey: `${namespace}-secure-meta`,
  });

  pinia.use(
    createPersistedState({
      key: (storeId) => `${namespace}-${storeId}`,
      storage: import.meta.env.DEV
        ? localStorage
        : {
            getItem: (key) => secureStorage.get(key) as null | string,
            setItem: (key, value) => secureStorage.set(key, value),
          },
    }),
  );
  app.use(pinia);
  return pinia;
}

export * from './auth';
