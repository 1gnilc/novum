import { afterEach, describe, expect, it, vi } from 'vitest';

import { createAppRouterHistory } from '#/router/history';

describe('router history', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it.each([
    ['hash', '#/my'],
    ['history', '/my'],
  ])('uses %s history from the environment', (mode, href) => {
    vi.stubEnv('VITE_ROUTER_HISTORY', mode);

    expect(createAppRouterHistory().createHref('/my')).toBe(href);
  });
});
