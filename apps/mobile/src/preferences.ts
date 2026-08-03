const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const;

type AppLocale = (typeof SUPPORTED_LOCALES)[number];

interface Preferences {
  locale: AppLocale;
}

const DEFAULT_LOCALE: AppLocale = 'zh-CN';
const defaultPreferences: Preferences = {
  locale: DEFAULT_LOCALE,
};

export { DEFAULT_LOCALE, defaultPreferences, SUPPORTED_LOCALES };
export type { AppLocale, Preferences };
