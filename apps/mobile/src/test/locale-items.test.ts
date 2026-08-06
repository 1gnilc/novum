import { describe, expect, it } from 'vitest';

import {
  DEFAULT_LOCALE,
  getLocaleItem,
  LOCALE_ITEMS,
  SUPPORTED_LOCALES,
} from '#/locales/locale';

describe('mobile locale catalog', () => {
  it('keeps the confirmed locale order and default', () => {
    expect(SUPPORTED_LOCALES).toEqual(['en-US', 'zh-CN', 'ha-NG', 'yo-NG']);
    expect(DEFAULT_LOCALE).toBe('en-US');
    expect(LOCALE_ITEMS.map(({ name }) => name)).toEqual([
      'English',
      '简体中文',
      'Hausa',
      'Yorùbá',
    ]);
  });

  it('returns locale metadata or null', () => {
    expect(getLocaleItem('en-US')).toMatchObject({
      locale: 'en-US',
      name: 'English',
    });
    expect(getLocaleItem('fr-FR')).toBeNull();
  });
});
