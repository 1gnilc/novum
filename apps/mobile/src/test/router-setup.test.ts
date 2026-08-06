import { createApp, nextTick } from 'vue';

import { describe, expect, it } from 'vitest';

import { BasicLayout } from '#/layouts';
import { loadLocaleMessages, setupI18n } from '#/locales';
import { router, setupRouter } from '#/router';
import { routes } from '#/router/routes';
import { initStores, usePreferences } from '#/stores';

describe('router setup', () => {
  it('loads application pages through the basic layout', () => {
    const [rootRoute] = routes;

    expect(rootRoute?.component).toBe(BasicLayout);
    expect(rootRoute?.children?.map(({ path }) => path)).toEqual([
      '',
      'market',
      'team',
      'fund',
      'my',
      'login',
      ':pathMatch(.*)*',
    ]);
    expect(rootRoute?.children?.map(({ name }) => name)).toEqual([
      'home',
      'market',
      'team',
      'fund',
      'my',
      'login',
      'not-found',
    ]);
    const children = rootRoute?.children ?? [];
    expect(
      children.find(({ name }) => name === 'market')?.meta?.requiresAuth,
    ).toBeUndefined();
    for (const name of ['team', 'fund', 'my']) {
      expect(
        children.find((route) => route.name === name)?.meta?.requiresAuth,
      ).toBe(true);
    }
  });

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

    await router.push('/my');
    await nextTick();
    expect(document.title).toBe('我的 - Novum Mobile');

    preferences.setLocale('en-US');
    await loadLocaleMessages('en-US');
    await nextTick();
    expect(document.title).toBe('My - Novum Mobile');
  });
});
