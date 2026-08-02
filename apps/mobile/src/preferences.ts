import type { SupportedLanguagesType } from '@vben/preferences';

import { defineOverridesPreferences } from '@vben/preferences';

export type AppLocale = SupportedLanguagesType;

export const DEFAULT_LOCALE: AppLocale = 'zh-CN';

export const overridesPreferences = defineOverridesPreferences({
  app: {
    enableRefreshToken: true,
    locale: DEFAULT_LOCALE,
  },
  theme: {
    mode: 'light',
  },
});
