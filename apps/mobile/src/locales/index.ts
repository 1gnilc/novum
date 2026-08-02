import type { App } from 'vue';

import { watch } from 'vue';
import { createI18n } from 'vue-i18n';

import { StorageManager } from '@vben/utils/cache';

import dayjs from 'dayjs';
import { Locale } from 'vant';
import enUSVant from 'vant/es/locale/lang/en-US';
import zhCNVant from 'vant/es/locale/lang/zh-CN';

import enUS from './langs/en-US/common.json';
import zhCN from './langs/zh-CN/common.json';

import 'dayjs/locale/en';
import 'dayjs/locale/zh-cn';

export const DEFAULT_LOCALE = 'zh-CN';
export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const;

export type AppLocale = (typeof SUPPORTED_LOCALES)[number];
type LocaleStorage = Pick<StorageManager, 'getItem' | 'setItem'>;

const storage = new StorageManager({ prefix: 'novum-mobile' });
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

let stopLocaleSync: (() => void) | undefined;

export async function loadLocale(target: LocaleStorage = storage) {
  const locale = await target.getItem<string>('locale');
  return isAppLocale(locale) ? locale : DEFAULT_LOCALE;
}

export async function saveLocale(
  locale: AppLocale,
  target: LocaleStorage = storage,
) {
  await target.setItem('locale', locale);
}

export async function setupI18n(app: App, locale?: AppLocale) {
  const current = locale ?? (await loadLocale());
  i18n.global.locale.value = current;
  app.use(i18n);
  stopLocaleSync?.();
  stopLocaleSync = watch(getLocale, syncLocale, { immediate: true });
  return i18n;
}

export function getLocale(): AppLocale {
  const locale = i18n.global.locale.value;
  return isAppLocale(locale) ? locale : DEFAULT_LOCALE;
}

export async function setLocale(locale: AppLocale) {
  i18n.global.locale.value = locale;
  await saveLocale(locale);
}

function syncLocale(locale: AppLocale) {
  document.documentElement.lang = locale;
  Locale.use(locale, vantLocales[locale]);
  dayjs.locale(dayjsLocales[locale]);
}

function isAppLocale(locale: unknown): locale is AppLocale {
  return SUPPORTED_LOCALES.includes(locale as AppLocale);
}

export { $t, $te };
