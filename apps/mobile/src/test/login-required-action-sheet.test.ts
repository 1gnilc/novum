import { flushPromises, mount } from '@vue/test-utils';
import { createApp, defineComponent, nextTick } from 'vue';
import { createMemoryHistory, createRouter, RouterView } from 'vue-router';

import { createPinia, setActivePinia } from 'pinia';
import { ActionSheet } from 'vant';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import LoginRequiredActionSheet from '#/components/authentication/login-required-action-sheet.vue';
import { DEFAULT_LOCALE, setupI18n } from '#/locales';
import { useAuthStore } from '#/stores';

vi.mock('#/router', () => ({
  router: {
    currentRoute: { value: { fullPath: '/' } },
    replace: vi.fn(),
  },
}));

describe('login required action sheet', () => {
  const page = defineComponent({ template: '<main>Page</main>' });

  beforeEach(() => {
    document.body.innerHTML = '<div id="test-root"></div>';
  });

  it('shows after entering a protected route and resets dismissal on navigation', async () => {
    const { auth, router, wrapper } = await mountSheet('/account');
    const sheet = wrapper.findComponent(ActionSheet);

    expect(wrapper.findComponent(RouterView).exists()).toBe(false);
    expect(sheet.props('show')).toBe(true);

    sheet.vm.$emit('cancel');
    await nextTick();
    expect(sheet.props('show')).toBe(false);

    await router.push('/account?tab=profile');
    await nextTick();
    expect(sheet.props('show')).toBe(true);

    auth.setAccessToken('access');
    await nextTick();
    expect(sheet.props('show')).toBe(false);
  });

  it('opens the public login route with the current full path as redirect', async () => {
    const { router, wrapper } = await mountSheet('/account?tab=profile');
    const sheet = wrapper.findComponent(ActionSheet);

    sheet.vm.$emit('select', { name: 'Go', value: 'login' });
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/login');
    expect(router.currentRoute.value.query.redirect).toBe(
      '%2Faccount%3Ftab%3Dprofile',
    );
  });

  async function mountSheet(path: string) {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { component: page, path: '/login' },
        {
          component: page,
          meta: { requiresAuth: true },
          path: '/account',
        },
      ],
    });
    await router.push(path);
    await router.isReady();
    const pinia = createPinia();
    setActivePinia(pinia);
    const i18n = await setupI18n(createApp(page), DEFAULT_LOCALE);
    const wrapper = mount(LoginRequiredActionSheet, {
      attachTo: '#test-root',
      global: { plugins: [pinia, router, i18n] },
    });
    return { auth: useAuthStore(), router, wrapper };
  }
});
