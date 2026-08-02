import { flushPromises, mount } from '@vue/test-utils';
import { createApp } from 'vue';
import { createMemoryHistory, createRouter } from 'vue-router';

import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { DEFAULT_LOCALE, setupI18n } from '#/locales';
import { useAuthStore } from '#/stores';
import AccountView from '#/views/account.vue';

const api = vi.hoisted(() => ({
  getCustomerUserInfo: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}));

vi.mock('#/api/core', () => api);
vi.mock('#/router', () => ({
  router: {
    currentRoute: { value: { fullPath: '/account' } },
    replace: vi.fn(),
  },
}));

describe('account view', () => {
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
  });

  async function mountView(authenticated = false) {
    const pinia = createPinia();
    setActivePinia(pinia);
    const auth = useAuthStore();
    if (authenticated) {
      auth.setAccessToken('access');
      auth.setRefreshToken('refresh');
    }
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ component: AccountView, path: '/account' }],
    });
    await router.push('/account');
    await router.isReady();
    const i18n = await setupI18n(createApp({}), DEFAULT_LOCALE);
    const wrapper = mount(AccountView, {
      global: {
        plugins: [pinia, router, i18n],
      },
    });
    await flushPromises();
    return { auth, wrapper };
  }
});
