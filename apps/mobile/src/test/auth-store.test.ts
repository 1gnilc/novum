import { createApp, nextTick } from 'vue';

import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { initStores, useAuthStore } from '#/stores';

const mocks = vi.hoisted(() => ({
  getCustomerUserInfo: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  replace: vi.fn(),
}));

vi.mock('#/api/core', () => ({
  getCustomerUserInfo: mocks.getCustomerUserInfo,
  login: mocks.login,
  logout: mocks.logout,
}));

vi.mock('#/router', () => ({
  router: {
    currentRoute: { value: { fullPath: '/account' } },
    replace: mocks.replace,
  },
}));

describe('useAuthStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  it('sets and clears the token pair through public actions', () => {
    const auth = useAuthStore();

    auth.setAccessToken('access');
    auth.setRefreshToken('refresh');

    expect(auth.accessToken).toBe('access');
    expect(auth.refreshToken).toBe('refresh');
    expect(auth.authenticated).toBe(true);

    auth.setAccessToken(null);
    auth.setRefreshToken(null);

    expect(auth.accessToken).toBeNull();
    expect(auth.refreshToken).toBeNull();
    expect(auth.authenticated).toBe(false);
  });

  it('logs in, stores the token pair, and loads current customer info', async () => {
    mocks.login.mockResolvedValue({
      accessToken: 'access',
      refreshToken: 'refresh',
    });
    mocks.getCustomerUserInfo.mockResolvedValue({
      id: '7',
      nickname: 'Customer',
      roleCodes: ['customer'],
      userId: '8',
      username: 'customer',
    });
    const auth = useAuthStore();

    const result = await auth.login({
      password: ' 123456 ',
      username: ' customer ',
    });

    expect(mocks.login).toHaveBeenCalledWith('customer', '123456');
    expect(auth.accessToken).toBe('access');
    expect(auth.refreshToken).toBe('refresh');
    expect(auth.authenticated).toBe(true);
    expect(auth.userInfo?.username).toBe('customer');
    expect(result.userInfo).toEqual(auth.userInfo);
    expect(auth.loginLoading).toBe(false);
  });

  it('clears a newly created session when current customer loading fails', async () => {
    mocks.login.mockResolvedValue({
      accessToken: 'access',
      refreshToken: 'refresh',
    });
    mocks.getCustomerUserInfo.mockRejectedValue(new Error('user info failed'));
    const auth = useAuthStore();

    await expect(
      auth.login({ password: '123456', username: 'customer' }),
    ).rejects.toThrow('user info failed');

    expect(auth.accessToken).toBeNull();
    expect(auth.refreshToken).toBeNull();
    expect(auth.userInfo).toBeNull();
    expect(auth.loginLoading).toBe(false);
  });

  it('clears local state and replaces the route even when remote logout fails', async () => {
    mocks.logout.mockRejectedValue(new Error('offline'));
    const auth = useAuthStore();
    auth.setAccessToken('access');
    auth.setRefreshToken('refresh');

    await auth.logout();

    expect(mocks.logout).toHaveBeenCalledWith('refresh');
    expect(auth.authenticated).toBe(false);
    expect(auth.userInfo).toBeNull();
    expect(mocks.replace).toHaveBeenCalledWith({
      path: '/login',
      query: { redirect: '%2Faccount' },
    });
  });

  it('persists only the token pair and restores it until logout', async () => {
    const namespace = 'custom-mobile';
    const auth = await createPersistedAuth(namespace);
    auth.setAccessToken('access');
    auth.setRefreshToken('refresh');
    auth.loginLoading = true;
    auth.userInfo = {
      createTime: '2026-08-03T00:00:00Z',
      id: '7',
      nickname: 'Customer',
      roleCodes: ['customer'],
      userId: '8',
      username: 'customer',
    };
    await nextTick();

    expect(
      JSON.parse(localStorage.getItem(`${namespace}-auth`) || '{}'),
    ).toEqual({
      accessToken: 'access',
      refreshToken: 'refresh',
    });

    const restored = await createPersistedAuth(namespace);
    expect(restored.accessToken).toBe('access');
    expect(restored.refreshToken).toBe('refresh');
    expect(restored.authenticated).toBe(true);
    expect(restored.userInfo).toBeNull();
    expect(restored.loginLoading).toBe(false);

    mocks.logout.mockResolvedValue(undefined);
    await restored.logout(false);

    const cleared = await createPersistedAuth(namespace);
    expect(cleared.accessToken).toBeNull();
    expect(cleared.refreshToken).toBeNull();
    expect(cleared.authenticated).toBe(false);
  });

  async function createPersistedAuth(namespace: string) {
    const pinia = await initStores(createApp({}), { namespace });
    setActivePinia(pinia);
    return useAuthStore();
  }
});
