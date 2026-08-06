import { flushPromises, mount } from '@vue/test-utils';
import { createApp } from 'vue';

import { ActionSheet } from 'vant';
import { describe, expect, it, vi } from 'vitest';

import Locale from '#/components/locale/index.vue';
import { $t, setupI18n } from '#/locales';
import { initStores, usePreferences } from '#/stores';

describe('locale selector', () => {
  it('shows all supported locales in order and switches the active locale', async () => {
    const app = createApp({});
    await initStores(app, { namespace: 'mobile-locale-component-test' });
    const preferences = usePreferences();
    preferences.setLocale('zh-CN');
    const i18n = await setupI18n(app);
    const wrapper = mount(Locale, { global: { plugins: [i18n] } });

    await wrapper.get('button').trigger('click');
    const sheet = wrapper.findComponent(ActionSheet);
    expect(sheet.props('actions')).toEqual([
      expect.objectContaining({ name: 'English', value: 'en-US' }),
      expect.objectContaining({
        color: 'var(--color-primary)',
        name: '简体中文',
        value: 'zh-CN',
      }),
      expect.objectContaining({ name: 'Hausa', value: 'ha-NG' }),
      expect.objectContaining({ name: 'Yorùbá', value: 'yo-NG' }),
    ]);

    sheet.vm.$emit('select', { name: 'Yorùbá', value: 'yo-NG' });
    await flushPromises();

    expect(preferences.locale).toBe('yo-NG');
    await vi.waitFor(() => {
      expect($t('my.title')).toBe('Tèmi');
      expect(sheet.props('show')).toBe(false);
    });
  });
});
