import { createApp, nextTick } from 'vue';

import dayjs from 'dayjs';
import { useCurrentLang } from 'vant';
import { describe, expect, it } from 'vitest';

import { $t, $te, DEFAULT_LOCALE, setLocale, setupI18n } from '#/locales';

describe('locale setup', () => {
  it('synchronizes HTML, Vant, and Day.js locales', async () => {
    await setupI18n(createApp({}), DEFAULT_LOCALE);
    await nextTick();

    expect(document.documentElement.lang).toBe('zh-CN');
    expect(useCurrentLang().value).toBe('zh-CN');
    expect(dayjs.locale()).toBe('zh-cn');
    expect($te('account.title')).toBe(true);
    expect($t('account.title')).toBe('账户');

    await setLocale('en-US');
    await nextTick();

    expect(document.documentElement.lang).toBe('en-US');
    expect(useCurrentLang().value).toBe('en-US');
    expect(dayjs.locale()).toBe('en');
    expect($t('account.title')).toBe('Account');
  });
});
