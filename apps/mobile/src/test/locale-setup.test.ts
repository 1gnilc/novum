import { createApp } from 'vue';

import {
  initPreferences,
  preferences,
  updatePreferences,
} from '@vben/preferences';

import dayjs from 'dayjs';
import { useCurrentLang } from 'vant';
import { describe, expect, it } from 'vitest';

import { $t, $te, loadLocaleMessages, setupI18n } from '#/locales';
import { overridesPreferences } from '#/preferences';

describe('locale setup', () => {
  it('synchronizes HTML, Vant, and Day.js locales', async () => {
    await initPreferences({
      namespace: 'mobile-locale-setup-test',
      overrides: overridesPreferences,
    });
    await setupI18n(createApp({}));

    expect(document.documentElement.lang).toBe('zh-CN');
    expect(useCurrentLang().value).toBe('zh-CN');
    expect(dayjs.locale()).toBe('zh-cn');
    expect(preferences.app.locale).toBe('zh-CN');
    expect($te('account.title')).toBe(true);
    expect($t('account.title')).toBe('账户');

    updatePreferences({ app: { locale: 'en-US' } });
    await loadLocaleMessages('en-US');

    expect(document.documentElement.lang).toBe('en-US');
    expect(useCurrentLang().value).toBe('en-US');
    expect(dayjs.locale()).toBe('en');
    expect(preferences.app.locale).toBe('en-US');
    expect($t('account.title')).toBe('Account');
  });
});
