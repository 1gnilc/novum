import { mount } from '@vue/test-utils';

import { createPinia, setActivePinia } from 'pinia';
import { ConfigProvider } from 'vant';
import { describe, expect, it } from 'vitest';

import App from '#/app.vue';
import { useThemeStore } from '#/stores';

describe('application theme provider', () => {
  it('provides the resolved theme to Vant', async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const themeStore = useThemeStore();
    themeStore.theme = 'dark';
    const wrapper = mount(App, {
      global: {
        plugins: [pinia],
        stubs: { BasicLayout: true, RouterView: true },
      },
    });

    const provider = wrapper.findComponent(ConfigProvider);
    expect(provider.props('theme')).toBe('dark');

    themeStore.theme = 'light';
    await wrapper.vm.$nextTick();
    expect(provider.props('theme')).toBe('light');
  });
});
