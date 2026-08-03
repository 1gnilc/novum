import type { PersistenceOptions } from 'pinia-plugin-persistedstate';

import type { AppLocale, Preferences } from '#/preferences';

import { mergeWithArrayOverride } from '@vben-core/shared/utils';

import { defineStore } from 'pinia';

import { defaultPreferences, SUPPORTED_LOCALES } from '#/preferences';
import { getLocale } from '#/utils/locale';

function resolvePreferences(cachedPreferences: Partial<Preferences> = {}) {
  const resolved = mergeWithArrayOverride(
    {},
    cachedPreferences,
    defaultPreferences,
  ) as Preferences;

  if (!isAppLocale(cachedPreferences.locale)) {
    resolved.locale = getLocale(
      SUPPORTED_LOCALES,
      defaultPreferences.locale,
    ) as AppLocale;
  }

  return resolved;
}

const usePreferences = defineStore('preferences', {
  actions: {
    setLocale(locale: AppLocale) {
      this.locale = locale;
    },
  },
  persist: {
    afterHydrate: ({ store }) => {
      const preferences = resolvePreferences(
        store.$state as Partial<Preferences>,
      );
      store.$patch((state) => {
        Object.assign(state, preferences);
      });
    },
  } satisfies PersistenceOptions<Preferences>,
  state: () => resolvePreferences(),
});

function isAppLocale(locale: unknown): locale is AppLocale {
  return SUPPORTED_LOCALES.includes(locale as AppLocale);
}

export { usePreferences };
