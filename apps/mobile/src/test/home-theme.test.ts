import { mount } from '@vue/test-utils';
import { createApp } from 'vue';

import { createPinia, setActivePinia } from 'pinia';
import { Radio, RadioGroup } from 'vant';
import { describe, expect, it } from 'vitest';

import { setupI18n } from '#/locales';
import { useThemeStore } from '#/stores';
import Home from '#/views/home.vue';

describe('home theme example', () => {
  it('offers all three themes and updates the theme store', async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const i18n = await setupI18n(createApp({}));

    const wrapper = mount(Home, {
      global: {
        plugins: [pinia, i18n],
        stubs: { LanguageSelector: true },
      },
    });

    expect(
      wrapper.findAllComponents(Radio).map((radio) => radio.props('name')),
    ).toEqual(['light', 'dark', 'system']);

    wrapper.findComponent(RadioGroup).vm.$emit('update:modelValue', 'dark');
    await wrapper.vm.$nextTick();
    expect(useThemeStore().theme).toBe('dark');
  });
});
