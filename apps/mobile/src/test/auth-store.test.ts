import { createApp } from 'vue';

import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { initStores, useAuthStore } from '#/stores';

const mocks = vi.hoisted(() => ({
  getUserInfo: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  replace: vi.fn(),
}));

vi.mock('#/api/session', () => ({
  getUserInfo: mocks.getUserInfo,
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

  it('logs in, stores the token pair, and loads current customer info', async () => {
    mocks.login.mockResolvedValue({
      accessToken: 'access',
      refreshToken: 'refresh',
    });
    mocks.getUserInfo.mockResolvedValue({
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
    mocks.getUserInfo.mockRejectedValue(new Error('user info failed'));
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
    auth.$patch({ accessToken: 'access', refreshToken: 'refresh' });

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
    const auth = await createPersistedAuth();
    auth.$patch({
      accessToken: 'access',
      loginLoading: true,
      refreshToken: 'refresh',
      userInfo: { nickname: 'Customer', username: 'customer' },
    });

    expect(
      JSON.parse(localStorage.getItem('novum-mobile-auth') || '{}'),
    ).toEqual({
      accessToken: 'access',
      refreshToken: 'refresh',
    });

    const restored = await createPersistedAuth();
    expect(restored.accessToken).toBe('access');
    expect(restored.refreshToken).toBe('refresh');
    expect(restored.authenticated).toBe(true);
    expect(restored.userInfo).toBeNull();
    expect(restored.loginLoading).toBe(false);

    mocks.logout.mockResolvedValue(undefined);
    await restored.logout(false);

    const cleared = await createPersistedAuth();
    expect(cleared.accessToken).toBeNull();
    expect(cleared.refreshToken).toBeNull();
    expect(cleared.authenticated).toBe(false);
  });

  async function createPersistedAuth() {
    const pinia = await initStores(createApp({}));
    setActivePinia(pinia);
    return useAuthStore();
  }
});
