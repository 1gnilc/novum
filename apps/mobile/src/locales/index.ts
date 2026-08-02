import type { App } from 'vue';

import type { AppLocale } from '#/preferences';

import { createI18n } from 'vue-i18n';

import { preferences, updatePreferences } from '@vben/preferences';

import dayjs from 'dayjs';
import { Locale } from 'vant';
import enUSVant from 'vant/es/locale/lang/en-US';
import zhCNVant from 'vant/es/locale/lang/zh-CN';

import { DEFAULT_LOCALE } from '#/preferences';

import enUS from './langs/en-US/common.json';
import zhCN from './langs/zh-CN/common.json';

import 'dayjs/locale/en';
import 'dayjs/locale/zh-cn';

export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const;

export type { AppLocale } from '#/preferences';
export { DEFAULT_LOCALE } from '#/preferences';

const messages = {
  'en-US': enUS,
  'zh-CN': zhCN,
};
const dayjsLocales = { 'en-US': 'en', 'zh-CN': 'zh-cn' } as const;
const vantLocales = { 'en-US': enUSVant, 'zh-CN': zhCNVant } as const;
const i18n = createI18n({
  fallbackLocale: DEFAULT_LOCALE,
  legacy: false,
  locale: DEFAULT_LOCALE,
  messages,
});
const $t = i18n.global.t;
const $te = i18n.global.te;

export async function setupI18n(app: App) {
  app.use(i18n);
  const locale = isAppLocale(preferences.app.locale)
    ? preferences.app.locale
    : DEFAULT_LOCALE;
  if (locale !== preferences.app.locale) {
    updatePreferences({ app: { locale } });
  }
  await loadLocaleMessages(locale);
  return i18n;
}

export async function loadLocaleMessages(locale: AppLocale) {
  i18n.global.locale.value = locale;
  document.documentElement.lang = locale;
  Locale.use(locale, vantLocales[locale]);
  dayjs.locale(dayjsLocales[locale]);
}

function isAppLocale(locale: unknown): locale is AppLocale {
  return SUPPORTED_LOCALES.includes(locale as AppLocale);
}

export { $t, $te };
