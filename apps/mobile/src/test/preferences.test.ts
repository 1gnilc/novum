import { createApp, defineComponent } from 'vue';

import { describe, expect, it, vi } from 'vitest';

import { initStores, usePreferences } from '#/stores';

const TestRoot = defineComponent({ name: 'TestRoot', render: () => null });

describe('mobile preferences', () => {
  it('lets the cached locale override the runtime default', async () => {
    const namespace = 'mobile-preferences-test';
    localStorage.setItem(
      `${namespace}-preferences`,
      JSON.stringify({ locale: 'en-US' }),
    );

    await initStores(createApp(TestRoot), { namespace });
    const preferences = usePreferences();

    expect(preferences.locale).toBe('en-US');
  });

  it('uses the runtime locale when the cached locale is empty', async () => {
    const namespace = 'mobile-preferences-runtime-locale-test';
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['en-US']);
    localStorage.setItem(
      `${namespace}-preferences`,
      JSON.stringify({ locale: '' }),
    );

    await initStores(createApp(TestRoot), { namespace });
    const preferences = usePreferences();

    expect(preferences.locale).toBe('en-US');
  });

  it('uses the default locale when the runtime locale is unsupported', async () => {
    const namespace = 'mobile-preferences-default-locale-test';
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['fr-FR']);

    await initStores(createApp(TestRoot), { namespace });

    expect(usePreferences().locale).toBe('zh-CN');
  });

  it('persists updates under the application namespace', async () => {
    const namespace = 'mobile-preferences-persist-test';
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['zh-CN']);
    await initStores(createApp(TestRoot), { namespace });
    const preferences = usePreferences();

    preferences.setLocale('en-US');

    await vi.waitFor(() => {
      const stored = localStorage.getItem(`${namespace}-preferences`);
      expect(stored && JSON.parse(stored).locale).toBe('en-US');
    });
  });
});
