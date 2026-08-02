import { mount } from '@vue/test-utils';
import { defineComponent, nextTick } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';

import dayjs from 'dayjs';
import { createPinia, setActivePinia } from 'pinia';
import { useCurrentLang } from 'vant';
import { describe, expect, it, vi } from 'vitest';

import App from '#/app.vue';
import { DEFAULT_LOCALE, setLocale, setupI18n } from '#/locales';

vi.mock('#/router', () => ({
  router: {
    currentRoute: { value: { fullPath: '/' } },
    replace: vi.fn(),
  },
}));

describe('app locale', () => {
  it('synchronizes HTML, Vant, and Day.js locales', async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          component: defineComponent({ template: '<main>Home</main>' }),
          meta: { title: 'home.title' },
          path: '/',
        },
      ],
    });
    await router.push('/');
    await router.isReady();
    mount(App, {
      global: {
        plugins: [pinia, router, setupI18n(DEFAULT_LOCALE)],
      },
    });
    await nextTick();

    expect(document.documentElement.lang).toBe('zh-CN');
    expect(useCurrentLang().value).toBe('zh-CN');
    expect(dayjs.locale()).toBe('zh-cn');

    await setLocale('en-US');
    await nextTick();

    expect(document.documentElement.lang).toBe('en-US');
    expect(useCurrentLang().value).toBe('en-US');
    expect(dayjs.locale()).toBe('en');
  });
});
