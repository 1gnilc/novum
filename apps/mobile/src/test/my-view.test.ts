import { flushPromises, mount } from '@vue/test-utils';
import { createApp } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';

import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { setupI18n } from '#/locales';
import { useAuthStore, usePreferences } from '#/stores';
import MyView from '#/views/my.vue';

const api = vi.hoisted(() => ({
  getCustomerUserInfo: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('#/api/core', () => api);
vi.mock('#/router', () => ({
  router: {
    currentRoute: { value: { fullPath: '/my' } },
    replace: vi.fn(),
  },
}));

describe('my view behavior', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('does not request customer information for an anonymous visitor', async () => {
    const { wrapper } = await mountView();

    expect(wrapper.text()).toContain('登录后查看账户信息');
    expect(api.getCustomerUserInfo).not.toHaveBeenCalled();
  });

  it('loads customer information for an authenticated customer', async () => {
    api.getCustomerUserInfo.mockResolvedValue({
      avatar: 'images/2026/08/06/customer.png',
      avatarUrl: 'https://images.example.test/images/2026/08/06/customer.png',
      id: '1',
      nickname: 'Customer',
      roleCodes: ['customer'],
      userId: '2',
      username: 'customer',
    });
    const { auth, wrapper } = await mountView(true);
    await flushPromises();

    expect(api.getCustomerUserInfo).toHaveBeenCalledOnce();
    expect(auth.userInfo?.username).toBe('customer');
    expect(wrapper.text()).toContain('Customer');
    expect(wrapper.find('.my-page__avatar img').attributes('src')).toBe(
      'https://images.example.test/images/2026/08/06/customer.png',
    );
  });

  async function mountView(authenticated = false) {
    const pinia = createPinia();
    setActivePinia(pinia);
    usePreferences().setLocale('zh-CN');
    const auth = useAuthStore();
    if (authenticated) {
      auth.setAccessToken('access');
      auth.setRefreshToken('refresh');
    }
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ component: MyView, path: '/my' }],
    });
    await router.push('/my');
    await router.isReady();
    const i18n = await setupI18n(createApp({}));
    const wrapper = mount(MyView, {
      global: {
        plugins: [pinia, router, i18n],
      },
    });
    await flushPromises();
    return { auth, wrapper };
  }
});
