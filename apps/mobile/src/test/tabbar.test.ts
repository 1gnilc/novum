import { mount } from '@vue/test-utils';
import { createApp } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';

import { createPinia, setActivePinia } from 'pinia';
import { TabbarItem } from 'vant';
import { describe, expect, it } from 'vitest';

import Tabbar from '#/components/tabbar/index.vue';
import { setupI18n } from '#/locales';

describe('mobile tabbar', () => {
  it('uses the confirmed five destinations', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { component: { template: '<main />' }, name: 'home', path: '/' },
        {
          component: { template: '<main />' },
          name: 'market',
          path: '/market',
        },
        { component: { template: '<main />' }, name: 'team', path: '/team' },
        { component: { template: '<main />' }, name: 'fund', path: '/fund' },
        { component: { template: '<main />' }, name: 'my', path: '/my' },
      ],
    });
    await router.push('/market');
    await router.isReady();
    const pinia = createPinia();
    setActivePinia(pinia);
    const i18n = await setupI18n(createApp({}));
    const wrapper = mount(Tabbar, {
      global: { plugins: [pinia, router, i18n] },
    });

    expect(
      wrapper.findAllComponents(TabbarItem).map((item) => item.props('name')),
    ).toEqual(['home', 'market', 'team', 'fund', 'my']);
    expect(wrapper.text()).toContain('Market');
  });
});
