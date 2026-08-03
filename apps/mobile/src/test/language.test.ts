import { flushPromises, mount } from '@vue/test-utils';
import { createApp } from 'vue';

import { ActionSheet, Button } from 'vant';
import { describe, expect, it, vi } from 'vitest';

import LanguageSelector from '#/components/language/index.vue';
import { $t, setupI18n } from '#/locales';
import { initStores, usePreferences } from '#/stores';

describe('language selector', () => {
  it('switches and persists the selected locale', async () => {
    const app = createApp({});
    await initStores(app, { namespace: 'mobile-language-test' });
    const preferences = usePreferences();
    preferences.setLocale('zh-CN');
    const i18n = await setupI18n(app);
    const wrapper = mount(LanguageSelector, {
      global: { plugins: [i18n] },
    });

    await wrapper.findComponent(Button).trigger('click');
    const sheet = wrapper.findComponent(ActionSheet);
    expect(sheet.props('show')).toBe(true);

    sheet.vm.$emit('select', { name: 'English', value: 'en-US' });
    await flushPromises();

    expect(preferences.locale).toBe('en-US');
    await vi.waitFor(() => {
      expect($t('account.title')).toBe('Account');
      expect(sheet.props('show')).toBe(false);
    });
  });
});
