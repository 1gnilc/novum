import { flushPromises, mount } from '@vue/test-utils';
import { createApp, defineComponent } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';

import { createPinia, setActivePinia } from 'pinia';
import { describe, expect, it } from 'vitest';

import LoginRequired from '#/components/authentication/login-required.vue';
import { BasicLayout } from '#/layouts';
import { setupI18n } from '#/locales';
import { usePreferences } from '#/stores';

describe('basic layout', () => {
  it('renders the matched page and the global login prompt', async () => {
    const layoutModule = await BasicLayout();
    const layout = layoutModule.default;
    const page = defineComponent({
      name: 'LayoutExamplePage',
      template: '<main>Layout example</main>',
    });
    const root = defineComponent({ template: '<RouterView />' });
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          children: [{ component: page, path: '' }],
          component: BasicLayout,
          path: '/',
        },
      ],
    });
    await router.push('/');
    await router.isReady();

    const pinia = createPinia();
    setActivePinia(pinia);
    usePreferences().setLocale('zh-CN');
    const i18n = await setupI18n(createApp({}));
    const wrapper = mount(root, {
      global: { plugins: [pinia, router, i18n] },
    });
    await flushPromises();

    expect(wrapper.findComponent(layout).exists()).toBe(true);
    expect(wrapper.findComponent(page).exists()).toBe(true);
    expect(wrapper.findComponent(LoginRequired).exists()).toBe(true);
  });
});
