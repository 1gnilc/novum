import { createApp, nextTick } from 'vue';

import { describe, expect, it } from 'vitest';

import { DEFAULT_LOCALE, setLocale, setupI18n } from '#/locales';
import { router, setupRouter } from '#/router';

describe('router setup', () => {
  it('synchronizes the localized page title', async () => {
    const app = createApp({});
    await setupI18n(app, DEFAULT_LOCALE);
    await router.push('/');
    await setupRouter(app);
    await nextTick();

    expect(document.title).toBe('Novum - Novum Mobile');

    await router.push('/account');
    await nextTick();
    expect(document.title).toBe('账户 - Novum Mobile');

    await setLocale('en-US');
    await nextTick();
    expect(document.title).toBe('Account - Novum Mobile');
  });
});
