import { flushPromises, mount } from '@vue/test-utils';
import { createApp } from 'vue';

import { initPreferences, preferences } from '@vben/preferences';

import { ActionSheet, Button } from 'vant';
import { describe, expect, it } from 'vitest';

import LanguageSelector from '#/components/language/index.vue';
import { $t, setupI18n } from '#/locales';
import { overridesPreferences } from '#/preferences';

describe('language selector', () => {
  it('switches and persists the selected locale', async () => {
    await initPreferences({
      namespace: 'mobile-language-test',
      overrides: overridesPreferences,
    });
    const i18n = await setupI18n(createApp({}));
    const wrapper = mount(LanguageSelector, {
      global: { plugins: [i18n] },
    });

    await wrapper.findComponent(Button).trigger('click');
    const sheet = wrapper.findComponent(ActionSheet);
    expect(sheet.props('show')).toBe(true);

    sheet.vm.$emit('select', { name: 'English', value: 'en-US' });
    await flushPromises();

    expect(preferences.app.locale).toBe('en-US');
    expect($t('account.title')).toBe('Account');
    expect(sheet.props('show')).toBe(false);
  });
});
