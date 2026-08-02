import { MemoryStorageDriver, StorageManager } from '@vben/utils/cache';

import { describe, expect, it } from 'vitest';

import { DEFAULT_LOCALE, loadLocale, saveLocale } from '#/locales';

describe('mobile locale storage', () => {
  it('falls back to the default locale for missing or unsupported values', async () => {
    const storage = createStorage();

    expect(await loadLocale(storage)).toBe(DEFAULT_LOCALE);
    await storage.setItem('locale', 'fr-FR');
    expect(await loadLocale(storage)).toBe(DEFAULT_LOCALE);
  });

  it('persists supported locales independently from the auth store', async () => {
    const storage = createStorage();

    await saveLocale('en-US', storage);

    expect(await loadLocale(storage)).toBe('en-US');
    expect(await storage.keys()).toEqual(['locale']);
  });

  function createStorage() {
    return new StorageManager({
      driver: new MemoryStorageDriver(),
      prefix: 'mobile-locale-test',
    });
  }
});
