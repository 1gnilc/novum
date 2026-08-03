import { createApp, nextTick } from 'vue';

import { describe, expect, it } from 'vitest';

import { loadLocaleMessages, setupI18n } from '#/locales';
import { router, setupRouter } from '#/router';
import { initStores, usePreferences } from '#/stores';

describe('router setup', () => {
  it('synchronizes the localized page title', async () => {
    const app = createApp({});
    await initStores(app, { namespace: 'mobile-router-setup-test' });
    const preferences = usePreferences();
    preferences.setLocale('zh-CN');
    await setupI18n(app);
    await router.push('/');
    await setupRouter(app);
    await nextTick();

    expect(document.title).toBe('Novum - Novum Mobile');

    await router.push('/account');
    await nextTick();
    expect(document.title).toBe('账户 - Novum Mobile');

    preferences.setLocale('en-US');
    await loadLocaleMessages('en-US');
    await nextTick();
    expect(document.title).toBe('Account - Novum Mobile');
  });
});
