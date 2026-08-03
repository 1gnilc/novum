import { describe, expect, it } from 'vitest';

import { resolveRedirect } from '#/router/redirect';

describe('resolveRedirect', () => {
  it('accepts encoded and plain internal paths', () => {
    expect(resolveRedirect('%2Faccount%3Ftab%3Dprofile')).toBe(
      '/account?tab=profile',
    );
    expect(resolveRedirect('/account')).toBe('/account');
  });

  it('rejects external, protocol-relative, login, and malformed values', () => {
    expect(resolveRedirect('https://example.test/account')).toBe('/');
    expect(resolveRedirect('//example.test/account')).toBe('/');
    expect(resolveRedirect('/login?redirect=%2Faccount')).toBe('/');
    expect(resolveRedirect('%E0%A4%A')).toBe('/');
    expect(resolveRedirect(undefined)).toBe('/');
  });
});
