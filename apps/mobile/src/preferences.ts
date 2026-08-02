export type AppLocale = 'en-US' | 'zh-CN';

interface AppPreferences {
  enableRefreshToken: boolean;
  locale: AppLocale;
}

interface Preferences {
  app: AppPreferences;
}

export const DEFAULT_LOCALE: AppLocale = 'zh-CN';

export const preferences: Preferences = {
  app: {
    enableRefreshToken: true,
    locale: DEFAULT_LOCALE,
  },
};
