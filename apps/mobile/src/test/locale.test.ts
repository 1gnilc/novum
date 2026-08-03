import { afterEach, describe, expect, it, vi } from 'vitest';

import { getLocale } from '#/utils/locale';

describe('runtime locale resolution', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns the first supported browser locale', () => {
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue([
      'fr-FR',
      'zh-CN',
      'en-US',
    ]);

    expect(getLocale(['en-US', 'zh-CN'], 'en-US')).toBe('zh-CN');
  });

  it('prefers a later exact locale over an earlier regional variant', () => {
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['en-GB', 'zh-CN']);

    expect(getLocale(['zh-CN', 'en-US'], 'en-US')).toBe('zh-CN');
  });

  it('returns the requested fallback when no locale matches', () => {
    vi.spyOn(navigator, 'languages', 'get').mockReturnValue(['fr-FR']);

    expect(getLocale(['zh-CN'], 'zh-CN')).toBe('zh-CN');
    expect(getLocale([])).toBe('en-US');
  });
});
