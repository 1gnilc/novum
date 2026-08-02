import { createApp, nextTick } from 'vue';

import { initPreferences, updatePreferences } from '@vben/preferences';

import { describe, expect, it } from 'vitest';

import { loadLocaleMessages, setupI18n } from '#/locales';
import { overridesPreferences } from '#/preferences';
import { router, setupRouter } from '#/router';

describe('router setup', () => {
  it('synchronizes the localized page title', async () => {
    const app = createApp({});
    await initPreferences({
      namespace: 'mobile-router-setup-test',
      overrides: overridesPreferences,
    });
    await setupI18n(app);
    await router.push('/');
    await setupRouter(app);
    await nextTick();

    expect(document.title).toBe('Novum - Novum Mobile');

    await router.push('/account');
    await nextTick();
    expect(document.title).toBe('账户 - Novum Mobile');

    updatePreferences({ app: { locale: 'en-US' } });
    await loadLocaleMessages('en-US');
    await nextTick();
    expect(document.title).toBe('Account - Novum Mobile');
  });
});
