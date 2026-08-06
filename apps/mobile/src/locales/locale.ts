import enUSFlag from '#/assets/flags/en-US.png';
import haNGFlag from '#/assets/flags/ha-NG.png';
import yoNGFlag from '#/assets/flags/yo-NG.png';
import zhCNFlag from '#/assets/flags/zh-CN.png';

const SUPPORTED_LOCALES = ['en-US', 'zh-CN', 'ha-NG', 'yo-NG'] as const;

type AppLocale = (typeof SUPPORTED_LOCALES)[number];

const DEFAULT_LOCALE: AppLocale = 'en-US';

interface LocaleItem {
  flags: string;
  locale: AppLocale;
  name: string;
}

const LOCALE_ITEMS: LocaleItem[] = [
  { flags: enUSFlag, locale: 'en-US', name: 'English' },
  { flags: zhCNFlag, locale: 'zh-CN', name: '简体中文' },
  { flags: haNGFlag, locale: 'ha-NG', name: 'Hausa' },
  { flags: yoNGFlag, locale: 'yo-NG', name: 'Yorùbá' },
];

function getLocaleItem(locale: null | string | undefined) {
  return LOCALE_ITEMS.find((item) => item.locale === locale) ?? null;
}

export { DEFAULT_LOCALE, getLocaleItem, LOCALE_ITEMS, SUPPORTED_LOCALES };
export type { AppLocale, LocaleItem };
