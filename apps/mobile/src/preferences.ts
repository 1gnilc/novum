import type { AppLocale } from '#/locales/locale';

import { DEFAULT_LOCALE, SUPPORTED_LOCALES } from '#/locales/locale';

interface Preferences {
  locale: AppLocale;
}

const defaultPreferences: Preferences = {
  locale: DEFAULT_LOCALE,
};

export { DEFAULT_LOCALE, defaultPreferences, SUPPORTED_LOCALES };
export type { AppLocale, Preferences };
