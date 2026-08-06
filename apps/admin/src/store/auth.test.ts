import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useAuthStore } from './auth';

const api = vi.hoisted(() => ({
  getAdminUserInfo: vi.fn(),
  getMenuAccessCodes: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
}));
const dynamicMessages = vi.hoisted(() => ({
  clear: vi.fn(),
  load: vi.fn(),
}));
const router = vi.hoisted(() => ({
  currentRoute: { value: { fullPath: '/system/admin' } },
  push: vi.fn(),
  replace: vi.fn(),
}));
const stores = vi.hoisted(() => ({
  access: {
    accessCodes: [] as string[],
    loginExpired: false,
    refreshToken: null as null | string,
    setAccessCodes: vi.fn(),
    setAccessToken: vi.fn(),
    setLoginExpired: vi.fn(),
    setRefreshToken: vi.fn(),
  },
  resetAllStores: vi.fn(),
  user: {
    setUserInfo: vi.fn(),
  },
}));

vi.mock('vue-router', () => ({ useRouter: () => router }));
vi.mock('@vben/stores', () => ({
  resetAllStores: stores.resetAllStores,
  useAccessStore: () => stores.access,
  useUserStore: () => stores.user,
}));
vi.mock('#/api', () => api);
vi.mock('#/locales/dynamic', () => ({
  clearDynamicMessages: dynamicMessages.clear,
  loadDynamicMessages: dynamicMessages.load,
}));
vi.mock('#/locales', () => ({ $t: (key: string) => key }));
vi.mock('element-plus', () => ({ ElNotification: vi.fn() }));

describe('administrator session state', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    stores.access.accessCodes = [];
    stores.access.loginExpired = false;
    stores.access.refreshToken = null;
    stores.access.setAccessCodes.mockImplementation((codes: string[]) => {
      stores.access.accessCodes = codes;
    });
  });

  it('refreshes button access codes when restoring an existing session', async () => {
    api.getAdminUserInfo.mockResolvedValue({
      avatar: 'https://images.example.test/images/2026/08/06/admin.png',
      avatarObjectKey: 'images/2026/08/06/admin.png',
      roleCodes: ['admin', 'rbac:manager'],
      userId: '1',
      username: 'admin',
    });
    api.getMenuAccessCodes.mockResolvedValue([
      'system:admin:create',
      'system:role:create',
    ]);
    await useAuthStore().getUserInfo();

    expect(api.getMenuAccessCodes).toHaveBeenCalledOnce();
    expect(stores.access.accessCodes).toEqual([
      'system:admin:create',
      'system:role:create',
    ]);
    expect(stores.user.setUserInfo).toHaveBeenCalledWith(
      expect.objectContaining({
        avatar: 'https://images.example.test/images/2026/08/06/admin.png',
        avatarObjectKey: 'images/2026/08/06/admin.png',
      }),
    );
  });

  it('allows a clean retry after the login request fails', async () => {
    api.login
      .mockRejectedValueOnce(new Error('network unavailable'))
      .mockResolvedValueOnce({
        accessToken: 'access-2',
        refreshToken: 'refresh-2',
      });
    api.getAdminUserInfo.mockResolvedValue({
      homePath: '/dashboard',
      userId: '1',
      username: 'admin',
    });
    api.getMenuAccessCodes.mockResolvedValue(['system:admin:create']);
    dynamicMessages.load.mockResolvedValue(undefined);
    const auth = useAuthStore();

    await expect(
      auth.login({ password: 'wrong', username: 'admin' }),
    ).rejects.toThrow('network unavailable');
    expect(auth.loginLoading).toBe(false);

    await expect(
      auth.login({ password: 'correct', username: 'admin' }),
    ).resolves.toMatchObject({ userInfo: { username: 'admin' } });
    expect(auth.loginLoading).toBe(false);
    expect(api.login).toHaveBeenCalledTimes(2);
    expect(stores.access.setAccessToken).toHaveBeenCalledWith('access-2');
    expect(stores.access.setRefreshToken).toHaveBeenCalledWith('refresh-2');
    expect(router.push).toHaveBeenCalledWith('/dashboard');
  });

  it('clears a partial session when post-login initialization fails', async () => {
    api.login.mockResolvedValue({
      accessToken: 'partial-access',
      refreshToken: 'partial-refresh',
    });
    api.getAdminUserInfo.mockRejectedValue(new Error('profile unavailable'));
    api.getMenuAccessCodes.mockResolvedValue([]);
    const auth = useAuthStore();

    await expect(
      auth.login({ password: 'Strong#123', username: 'admin' }),
    ).rejects.toThrow('profile unavailable');

    expect(stores.resetAllStores).toHaveBeenCalledOnce();
    expect(stores.access.setLoginExpired).toHaveBeenCalledWith(false);
    expect(dynamicMessages.clear).toHaveBeenCalledOnce();
    expect(router.push).not.toHaveBeenCalled();
    expect(auth.loginLoading).toBe(false);
  });

  it('clears local state and redirects even when remote logout fails', async () => {
    stores.access.refreshToken = 'refresh-token';
    api.logout.mockRejectedValue(new Error('backend unavailable'));

    await useAuthStore().logout();

    expect(api.logout).toHaveBeenCalledWith('refresh-token');
    expect(stores.resetAllStores).toHaveBeenCalledOnce();
    expect(dynamicMessages.clear).toHaveBeenCalledOnce();
    expect(router.replace).toHaveBeenCalledWith({
      path: '/auth/login',
      query: { redirect: encodeURIComponent('/system/admin') },
    });
  });
});
