import {
  initPreferences,
  preferences,
  updatePreferences,
} from '@vben/preferences';

import { describe, expect, it, vi } from 'vitest';

import { overridesPreferences } from '#/preferences';

describe('mobile preferences', () => {
  it('initializes and persists the locale with the application namespace', async () => {
    const namespace = 'mobile-preferences-test';
    await initPreferences({ namespace, overrides: overridesPreferences });

    expect(preferences.app.enableRefreshToken).toBe(true);
    expect(preferences.app.locale).toBe('zh-CN');

    updatePreferences({ app: { locale: 'en-US' } });

    await vi.waitFor(() => {
      const stored = localStorage.getItem(`${namespace}-preferences-locale`);
      expect(stored && JSON.parse(stored).value).toBe('en-US');
    });
  });
});
