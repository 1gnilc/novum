import type { AxiosRequestConfig, AxiosResponse } from 'axios';

import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { baseRequestClient, requestClient } from '#/api/request';
import { login } from '#/api/session';
import { AuthenticationRequiredError } from '#/errors/authentication-required';
import { useAuthStore } from '#/stores';

vi.mock('#/router', () => ({
  router: {
    currentRoute: { value: { fullPath: '/' } },
    replace: vi.fn(),
  },
}));

describe('mobile request client', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    requestClient.isRefreshing = false;
    requestClient.refreshTokenQueue.splice(0);
  });

  it('rejects protected requests locally when no access token exists', async () => {
    const adapter = vi.fn();
    requestClient.instance.defaults.adapter = adapter;

    await expect(
      requestClient.get('/customer/user-info', {
        requestAuth: { required: true },
      }),
    ).rejects.toBeInstanceOf(AuthenticationRequiredError);

    expect(adapter).not.toHaveBeenCalled();
  });

  it('adds the bearer token and current locale to authenticated requests', async () => {
    const auth = useAuthStore();
    auth.$patch({ accessToken: 'access', refreshToken: 'refresh' });
    const adapter = vi.fn(async (config: AxiosRequestConfig) =>
      response(config, { code: 0, data: { ok: true } }),
    );
    requestClient.instance.defaults.adapter = adapter;

    await expect(
      requestClient.get('/customer/user-info', {
        requestAuth: { required: true },
      }),
    ).resolves.toEqual({ ok: true });

    const config = adapter.mock.calls[0]?.[0];
    expect(config?.headers?.Authorization).toBe('Bearer access');
    expect(config?.headers?.['Accept-Language']).toBe('zh-CN');
  });

  it('refreshes once, updates the token pair, and replays the failed request', async () => {
    const auth = useAuthStore();
    auth.$patch({ accessToken: 'old-access', refreshToken: 'refresh' });
    const adapter = vi.fn(async (config: AxiosRequestConfig) => {
      if (adapter.mock.calls.length === 1) {
        const error = new Error('Unauthorized') as Error & {
          config: AxiosRequestConfig;
          response: AxiosResponse;
        };
        error.config = config;
        error.response = response(config, null, 401);
        throw error;
      }
      return response(config, { code: 0, data: { ok: true } });
    });
    requestClient.instance.defaults.adapter = adapter;
    baseRequestClient.instance.defaults.adapter = vi.fn(async (config) =>
      response(config, {
        code: 0,
        data: { accessToken: 'new-access', refreshToken: 'refresh' },
      }),
    );

    await expect(
      requestClient.get('/customer/user-info', {
        requestAuth: { required: true },
      }),
    ).resolves.toEqual({ ok: true });

    expect(adapter).toHaveBeenCalledTimes(2);
    expect(adapter.mock.calls[1]?.[0].headers?.Authorization).toBe(
      'Bearer new-access',
    );
    expect(auth.accessToken).toBe('new-access');
    expect(auth.refreshToken).toBe('refresh');
  });

  it('clears the session when refreshing fails', async () => {
    const auth = useAuthStore();
    auth.$patch({
      accessToken: 'old-access',
      refreshToken: 'refresh',
      userInfo: { nickname: 'Customer', username: 'customer' },
    });
    requestClient.instance.defaults.adapter = vi.fn(async (config) => {
      throw unauthorized(config);
    });
    baseRequestClient.instance.defaults.adapter = vi.fn(async (config) => {
      throw unauthorized(config);
    });

    await expect(
      requestClient.get('/customer/user-info', {
        requestAuth: { required: true },
      }),
    ).rejects.toBeDefined();

    expect(auth.accessToken).toBeNull();
    expect(auth.refreshToken).toBeNull();
    expect(auth.userInfo).toBeNull();
  });

  it('clears the refreshed session when the replay is unauthorized', async () => {
    const auth = useAuthStore();
    auth.$patch({
      accessToken: 'old-access',
      refreshToken: 'refresh',
      userInfo: { nickname: 'Customer', username: 'customer' },
    });
    const adapter = vi.fn(async (config: AxiosRequestConfig) => {
      throw unauthorized(config);
    });
    requestClient.instance.defaults.adapter = adapter;
    baseRequestClient.instance.defaults.adapter = vi.fn(async (config) =>
      response(config, {
        code: 0,
        data: { accessToken: 'new-access', refreshToken: 'refresh' },
      }),
    );

    await expect(
      requestClient.get('/customer/user-info', {
        requestAuth: { required: true },
      }),
    ).rejects.toBeDefined();

    expect(adapter).toHaveBeenCalledTimes(2);
    expect(auth.accessToken).toBeNull();
    expect(auth.refreshToken).toBeNull();
    expect(auth.userInfo).toBeNull();
  });

  it('uses the request client for login and leaves the base client bare', async () => {
    const sessionAdapter = vi.fn(async (config: AxiosRequestConfig) =>
      response(config, {
        code: 0,
        data: { accessToken: 'access', refreshToken: 'refresh' },
      }),
    );
    const baseAdapter = vi.fn();
    requestClient.instance.defaults.adapter = sessionAdapter;
    baseRequestClient.instance.defaults.adapter = baseAdapter;

    await expect(login('customer', '123456')).resolves.toEqual({
      accessToken: 'access',
      refreshToken: 'refresh',
    });

    expect(sessionAdapter).toHaveBeenCalledOnce();
    expect(baseAdapter).not.toHaveBeenCalled();
    expect(sessionAdapter.mock.calls[0]?.[0].headers?.['Accept-Language']).toBe(
      'zh-CN',
    );
  });

  function response(
    config: AxiosRequestConfig,
    data: unknown,
    status = 200,
  ): AxiosResponse {
    return {
      config: config as AxiosResponse['config'],
      data,
      headers: {},
      status,
      statusText: status === 200 ? 'OK' : 'Unauthorized',
    };
  }

  function unauthorized(config: AxiosRequestConfig) {
    const error = new Error('Unauthorized') as Error & {
      config: AxiosRequestConfig;
      response: AxiosResponse;
    };
    error.config = config;
    error.response = response(config, null, 401);
    return error;
  }
});
