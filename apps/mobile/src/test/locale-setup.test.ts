import { createApp, defineComponent } from 'vue';

import dayjs from 'dayjs';
import { useCurrentLang } from 'vant';
import { describe, expect, it } from 'vitest';

import { $t, $te, coreSetup, loadLocaleMessages, setupI18n } from '#/locales';
import { initStores, usePreferences } from '#/stores';

const TestRoot = defineComponent({ name: 'TestRoot', render: () => null });

describe('locale setup', () => {
  it('loads and activates messages through coreSetup', async () => {
    await coreSetup(createApp(TestRoot), {
      defaultLocale: 'en-US',
      loadMessages: async (locale) => ({ probe: `message-${locale}` }),
    });

    expect(document.documentElement.lang).toBe('en-US');
    expect($t('probe')).toBe('message-en-US');
  });

  it('synchronizes HTML, Vant, and Day.js locales', async () => {
    const app = createApp(TestRoot);
    await initStores(app, { namespace: 'mobile-locale-setup-test' });
    const preferences = usePreferences();
    preferences.setLocale('zh-CN');
    await setupI18n(app);

    expect(document.documentElement.lang).toBe('zh-CN');
    expect(useCurrentLang().value).toBe('zh-CN');
    expect(dayjs.locale()).toBe('zh-cn');
    expect(preferences.locale).toBe('zh-CN');
    expect($te('account.title')).toBe(true);
    expect($t('account.title')).toBe('账户');

    preferences.setLocale('en-US');
    await loadLocaleMessages('en-US');

    expect(document.documentElement.lang).toBe('en-US');
    expect(useCurrentLang().value).toBe('en-US');
    expect(dayjs.locale()).toBe('en');
    expect(preferences.locale).toBe('en-US');
    expect($t('account.title')).toBe('Account');
  });
});
