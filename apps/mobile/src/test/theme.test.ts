import { createApp, defineComponent, nextTick } from 'vue';

import { createPinia, setActivePinia } from 'pinia';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { initStores, useThemeStore } from '#/stores';
import { getSystemTheme } from '#/utils/theme';

const TestRoot = defineComponent({ name: 'TestRoot', render: () => null });

describe('system theme detection', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it.each([
    ['dark', true, false],
    ['light', false, true],
  ] as const)('returns the %s system theme', (expected, dark, light) => {
    stubMatchMedia({ dark, light });

    expect(getSystemTheme()).toBe(expected);
  });

  it('returns null when the system theme is unavailable', () => {
    vi.stubGlobal('matchMedia', undefined);

    expect(getSystemTheme()).toBeNull();
  });

  it('returns null when neither color scheme matches', () => {
    stubMatchMedia({ dark: false, light: false });

    expect(getSystemTheme()).toBeNull();
  });
});

describe('useThemeStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('resolves explicit light and dark themes', () => {
    const store = useThemeStore();

    store.theme = 'light';
    expect(store.resolvedTheme).toBe('light');

    store.theme = 'dark';
    expect(store.resolvedTheme).toBe('dark');
  });

  it('follows the system theme and defaults to dark when unavailable', () => {
    const media = stubMatchMedia({ dark: false, light: true });
    const store = useThemeStore();

    expect(store.theme).toBe('system');
    expect(store.resolvedTheme).toBe('light');

    media.update({ dark: false, light: false });

    expect(store.resolvedTheme).toBe('dark');
  });

  it('updates while following a runtime system theme change', () => {
    const media = stubMatchMedia({ dark: false, light: true });
    const store = useThemeStore();

    expect(store.resolvedTheme).toBe('light');

    media.update({ dark: true, light: false });

    expect(store.theme).toBe('system');
    expect(store.resolvedTheme).toBe('dark');
  });

  it('persists the selected theme under the application namespace', async () => {
    const namespace = 'mobile-theme-persist-test';
    await initStores(createApp(TestRoot), { namespace });
    const store = useThemeStore();

    store.theme = 'light';
    await nextTick();

    expect(
      JSON.parse(localStorage.getItem(`${namespace}-theme`) || '{}'),
    ).toEqual({ theme: 'light' });

    const restoredPinia = await initStores(createApp(TestRoot), { namespace });
    setActivePinia(restoredPinia);
    expect(useThemeStore().theme).toBe('light');
  });
});

function stubMatchMedia({ dark, light }: { dark: boolean; light: boolean }) {
  const listeners = new Set<EventListener>();
  const state = { dark, light };
  vi.stubGlobal(
    'matchMedia',
    vi.fn((query: string) => ({
      addEventListener: (_: string, listener: EventListener) => {
        listeners.add(listener);
      },
      get matches() {
        return query.includes('dark') ? state.dark : state.light;
      },
      media: query,
      removeEventListener: (_: string, listener: EventListener) => {
        listeners.delete(listener);
      },
    })),
  );

  return {
    update(next: { dark: boolean; light: boolean }) {
      Object.assign(state, next);
      for (const listener of listeners) {
        listener(new Event('change'));
      }
    },
  };
}
