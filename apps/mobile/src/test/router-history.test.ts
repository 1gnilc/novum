import { afterEach, describe, expect, it, vi } from 'vitest';

describe('router history', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it.each([
    ['hash', '#/account'],
    ['history', '/account'],
  ])('uses %s history from the environment', async (mode, href) => {
    vi.resetModules();
    vi.stubEnv('VITE_ROUTER_HISTORY', mode);

    const { router } = await import('#/router');

    expect(router.options.history.createHref('/account')).toBe(href);
  });
});
