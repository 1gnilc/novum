import { flushPromises, mount } from '@vue/test-utils';
import { createApp } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';

import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { DEFAULT_LOCALE, setupI18n } from '#/locales';
import { useAuthStore } from '#/stores';
import LoginView from '#/views/login.vue';

const api = vi.hoisted(() => ({
  getUserInfo: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('#/api/session', () => api);
vi.mock('#/router', () => ({
  router: {
    currentRoute: { value: { fullPath: '/' } },
    replace: vi.fn(),
  },
}));

describe('login view', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('allows an authenticated customer to sign in again and follows the redirect', async () => {
    const pinia = createPinia();
    setActivePinia(pinia);
    const auth = useAuthStore();
    auth.$patch({ accessToken: 'old-access', refreshToken: 'old-refresh' });
    api.login.mockResolvedValue({
      accessToken: 'new-access',
      refreshToken: 'new-refresh',
    });
    api.getUserInfo.mockResolvedValue({
      id: '1',
      nickname: 'Customer',
      roleCodes: ['customer'],
      userId: '2',
      username: 'customer',
    });
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { component: LoginView, path: '/login' },
        { component: { template: '<main>Account</main>' }, path: '/account' },
      ],
    });
    await router.push({
      path: '/login',
      query: { redirect: encodeURIComponent('/account?tab=profile') },
    });
    await router.isReady();
    const i18n = await setupI18n(createApp({}), DEFAULT_LOCALE);
    const wrapper = mount(LoginView, {
      global: {
        plugins: [pinia, router, i18n],
      },
    });

    const inputs = wrapper.findAll('input');
    expect(inputs).toHaveLength(2);
    await inputs[0]?.setValue('customer');
    await inputs[1]?.setValue('123456');
    await wrapper.find('form').trigger('submit');
    await flushPromises();

    expect(api.login).toHaveBeenCalledWith('customer', '123456');
    expect(auth.accessToken).toBe('new-access');
    expect(router.currentRoute.value.fullPath).toBe('/account?tab=profile');
  });
});
