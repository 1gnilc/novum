import { flushPromises, mount } from '@vue/test-utils';

import { ActionSheet, Button } from 'vant';
import { describe, expect, it } from 'vitest';

import LanguageSelector from '#/components/language-selector.vue';
import { DEFAULT_LOCALE, getLocale, loadLocale, setupI18n } from '#/locales';

describe('language selector', () => {
  it('switches and persists the selected locale', async () => {
    const i18n = setupI18n(DEFAULT_LOCALE);
    const wrapper = mount(LanguageSelector, {
      global: { plugins: [i18n] },
    });

    await wrapper.findComponent(Button).trigger('click');
    const sheet = wrapper.findComponent(ActionSheet);
    expect(sheet.props('show')).toBe(true);

    sheet.vm.$emit('select', { name: 'English', value: 'en-US' });
    await flushPromises();

    expect(getLocale()).toBe('en-US');
    expect(await loadLocale()).toBe('en-US');
    expect(sheet.props('show')).toBe(false);
  });
});
