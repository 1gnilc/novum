/**
 * 该文件可自行根据业务逻辑进行调整
 */
import type { RequestClientOptions, RequestErrorType } from '@vben/request';

import { useAppConfig } from '@vben/hooks';
import { preferences } from '@vben/preferences';
import {
  authenticateResponseInterceptor,
  defaultResponseInterceptor,
  errorMessageResponseInterceptor,
  RequestClient,
} from '@vben/request';
import { useAccessStore } from '@vben/stores';

import { ElMessage } from 'element-plus';

import { $t } from '#/locales';
import { useAuthStore } from '#/store';

import { refresh } from './core';

const { apiURL } = useAppConfig(import.meta.env, import.meta.env.PROD);

const requestErrorMessages: Record<RequestErrorType, string> = {
  'bad-request': 'ui.fallback.http.badRequest',
  forbidden: 'ui.fallback.http.forbidden',
  'internal-server-error': 'ui.fallback.http.internalServerError',
  'network-error': 'ui.fallback.http.networkError',
  'not-found': 'ui.fallback.http.notFound',
  'request-timeout': 'ui.fallback.http.requestTimeout',
  unauthorized: 'ui.fallback.http.unauthorized',
};

function createRequestClient(baseURL: string, options?: RequestClientOptions) {
  const client = new RequestClient({
    ...options,
    baseURL,
  });

  /**
   * 重新认证逻辑
   */
  async function doReAuthenticate() {
    console.warn('Access token or refresh token is invalid or expired. ');
    const accessStore = useAccessStore();
    const authStore = useAuthStore();
    accessStore.setAccessToken(null);
    accessStore.setRefreshToken(null);
    if (
      preferences.app.loginExpiredMode === 'modal' &&
      accessStore.isAccessChecked
    ) {
      accessStore.setLoginExpired(true);
    } else {
      await authStore.logout();
    }
  }

  /**
   * 刷新token逻辑
   */
  async function doRefreshToken() {
    const accessStore = useAccessStore();
    const currentRefreshToken = accessStore.refreshToken;
    if (!currentRefreshToken) {
      throw new Error('Refresh token is missing.');
    }

    const session = await refresh(currentRefreshToken);
    accessStore.setAccessToken(session.accessToken);
    accessStore.setRefreshToken(session.refreshToken);
    return session.accessToken;
  }

  function formatToken(token: null | string) {
    return token ? `Bearer ${token}` : null;
  }

  // 请求头处理
  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const accessStore = useAccessStore();

      config.headers.Authorization = formatToken(accessStore.accessToken);
      config.headers['Accept-Language'] = preferences.app.locale;
      return config;
    },
  });

  // 处理返回的响应数据格式
  client.addResponseInterceptor(
    defaultResponseInterceptor({
      codeField: 'code',
      dataField: 'data',
      successCode: 0,
    }),
  );

  // token过期的处理
  client.addResponseInterceptor(
    authenticateResponseInterceptor({
      client,
      doReAuthenticate,
      doRefreshToken,
      enableRefreshToken: preferences.app.enableRefreshToken,
      formatToken,
    }),
  );

  // 通用的错误处理,如果没有进入上面的错误处理逻辑，就会进入这里
  client.addResponseInterceptor(
    errorMessageResponseInterceptor({
      onError: (message, error: any) => {
        const responseData = error?.response?.data ?? {};
        const responseMessage =
          responseData?.error ?? responseData?.message ?? '';
        ElMessage.error(responseMessage || message);
      },
      resolveMessage: (type) => $t(requestErrorMessages[type]),
    }),
  );

  return client;
}

export const requestClient = createRequestClient(apiURL, {
  responseReturn: 'data',
});

export const baseRequestClient = new RequestClient({ baseURL: apiURL });
