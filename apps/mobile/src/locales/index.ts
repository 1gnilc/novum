import type { App } from 'vue';

import type { AppLocale } from '#/preferences';

import { createI18n } from 'vue-i18n';

import dayjs from 'dayjs';
import { Locale } from 'vant';

import { DEFAULT_LOCALE, SUPPORTED_LOCALES } from '#/preferences';
import { usePreferences } from '#/stores';

type LocaleMessages = Record<string, unknown>;
type LoadMessages = (locale: AppLocale) => Promise<LocaleMessages>;

interface LocaleModule {
  default: LocaleMessages;
}

interface LocaleSetupOptions {
  defaultLocale?: AppLocale;
  loadMessages?: LoadMessages;
  missingWarn?: boolean;
}

const FALLBACK_LOCALE: AppLocale = 'en-US';
const modules = import.meta.glob<LocaleModule>('./langs/*/common.json');
const i18n = createI18n({
  fallbackLocale: FALLBACK_LOCALE,
  legacy: false,
  locale: '',
  messages: {},
});
let runtimeLoadMessages: LoadMessages = async () => ({});

async function coreSetup(app: App, options: LocaleSetupOptions = {}) {
  const {
    defaultLocale = DEFAULT_LOCALE,
    loadMessages = async () => ({}),
    missingWarn = false,
  } = options;
  runtimeLoadMessages = loadMessages;
  app.use(i18n);
  await loadLocaleMessages(defaultLocale);
  i18n.global.setMissingHandler((_, key) => {
    if (missingWarn && key.includes('.')) {
      console.warn(`[intlify] Not found '${key}' locale message.`);
    }
  });
}

async function buildMessages(locale: AppLocale) {
  const message = await modules[`./langs/${locale}/common.json`]?.();
  return message?.default ?? {};
}

async function loadMessages(locale: AppLocale) {
  const [messages] = await Promise.all([
    buildMessages(locale),
    loadThirdPartyMessage(locale),
  ]);
  return messages;
}

async function loadThirdPartyMessage(locale: AppLocale) {
  await Promise.all([loadVantLocale(locale), loadDayjsLocale(locale)]);
}

async function loadVantLocale(locale: AppLocale) {
  switch (locale) {
    case 'en-US': {
      const messages = await import('vant/es/locale/lang/en-US');
      Locale.use(locale, messages.default);
      break;
    }
    case 'zh-CN': {
      const messages = await import('vant/es/locale/lang/zh-CN');
      Locale.use(locale, messages.default);
      break;
    }
  }
}

async function loadDayjsLocale(locale: AppLocale) {
  switch (locale) {
    case 'en-US': {
      await import('dayjs/locale/en');
      dayjs.locale('en');
      break;
    }
    case 'zh-CN': {
      await import('dayjs/locale/zh-cn');
      dayjs.locale('zh-cn');
      break;
    }
  }
}

async function loadLocaleMessages(locale: AppLocale) {
  if (i18n.global.locale.value === locale) {
    return setI18nLanguage(locale);
  }

  const messages = await runtimeLoadMessages(locale);
  i18n.global.setLocaleMessage(locale, messages);
  return setI18nLanguage(locale);
}

function setI18nLanguage(locale: AppLocale) {
  i18n.global.locale.value = locale;
  document?.documentElement.setAttribute('lang', locale);
}

async function setupI18n(app: App, options: LocaleSetupOptions = {}) {
  const preferences = usePreferences();
  await coreSetup(app, {
    defaultLocale: preferences.locale,
    loadMessages,
    missingWarn: !import.meta.env.PROD,
    ...options,
  });
  i18n.global.fallbackLocale.value = FALLBACK_LOCALE;
  return i18n;
}

const $t = i18n.global.t;
const $te = i18n.global.te;

export {
  $t,
  $te,
  coreSetup,
  DEFAULT_LOCALE,
  loadLocaleMessages,
  setupI18n,
  SUPPORTED_LOCALES,
};
export type { AppLocale, LocaleSetupOptions };
