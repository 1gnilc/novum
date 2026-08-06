import { describe, expect, it } from 'vitest';

import { resolveRedirect } from '#/router/redirect';

describe('resolveRedirect', () => {
  it('accepts encoded and plain internal paths', () => {
    expect(resolveRedirect('%2Fmy%3Ftab%3Dprofile')).toBe('/my?tab=profile');
    expect(resolveRedirect('/my')).toBe('/my');
  });

  it('rejects external, protocol-relative, login, and malformed values', () => {
    expect(resolveRedirect('https://example.test/my')).toBe('/');
    expect(resolveRedirect('//example.test/my')).toBe('/');
    expect(resolveRedirect('/login?redirect=%2Fmy')).toBe('/');
    expect(resolveRedirect('%E0%A4%A')).toBe('/');
    expect(resolveRedirect(undefined)).toBe('/');
  });
});
