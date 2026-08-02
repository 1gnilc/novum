import type { Composer, I18n } from 'vue-i18n';

import { createI18n } from 'vue-i18n';

import { StorageManager } from '@vben/utils/cache';

import enUS from './langs/en-US/common.json';
import zhCN from './langs/zh-CN/common.json';

export const DEFAULT_LOCALE = 'zh-CN';
export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const;

export type AppLocale = (typeof SUPPORTED_LOCALES)[number];
type LocaleStorage = Pick<StorageManager, 'getItem' | 'setItem'>;

const storage = new StorageManager({ prefix: 'novum-mobile' });
const messages = {
  'en-US': enUS,
  'zh-CN': zhCN,
};

let i18n: I18n | undefined;

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

export function setupI18n(locale: AppLocale) {
  i18n = createI18n({
    fallbackLocale: DEFAULT_LOCALE,
    legacy: false,
    locale,
    messages,
  });
  return i18n;
}

export function getLocale(): AppLocale {
  const locale = composer()?.locale.value;
  return isAppLocale(locale) ? locale : DEFAULT_LOCALE;
}

export async function setLocale(locale: AppLocale) {
  const active = composer();
  if (active) {
    active.locale.value = locale;
  }
  await saveLocale(locale);
}

export function translate(key: string) {
  return composer()?.t(key) ?? key;
}

function composer() {
  return i18n?.global as Composer | undefined;
}

function isAppLocale(locale: unknown): locale is AppLocale {
  return SUPPORTED_LOCALES.includes(locale as AppLocale);
}
