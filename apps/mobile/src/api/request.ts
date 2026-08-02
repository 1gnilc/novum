import type { RequestClientOptions, RequestErrorType } from '@vben/request';

import {
  authenticateResponseInterceptor,
  defaultResponseInterceptor,
  errorMessageResponseInterceptor,
  RequestClient,
} from '@vben/request';

import { $t, getLocale } from '#/locales';
import { useAuthStore } from '#/stores';

import { refresh } from './session';

const requestErrorMessages: Record<RequestErrorType, string> = {
  'bad-request': 'request.badRequest',
  forbidden: 'request.forbidden',
  'internal-server-error': 'request.internalServerError',
  'network-error': 'request.networkError',
  'not-found': 'request.notFound',
  'request-timeout': 'request.requestTimeout',
  unauthorized: 'request.unauthorized',
};

const apiURL = import.meta.env.VITE_APP_API_URL || '/api';

function createRequestClient(baseURL: string, options?: RequestClientOptions) {
  const client = new RequestClient({ ...options, baseURL });

  async function doReAuthenticate() {
    useAuthStore().$reset();
  }

  async function doRefreshToken() {
    const auth = useAuthStore();
    if (!auth.refreshToken) {
      throw new Error('Refresh token is missing.');
    }
    const session = await refresh(auth.refreshToken);
    auth.setAccessToken(session.accessToken);
    auth.setRefreshToken(session.refreshToken);
    return session.accessToken;
  }

  function formatToken(token: null | string) {
    return token ? `Bearer ${token}` : null;
  }

  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const auth = useAuthStore();
      config.headers.Authorization = formatToken(auth.accessToken);
      config.headers['Accept-Language'] = getLocale();
      return config;
    },
  });

  client.addResponseInterceptor(
    defaultResponseInterceptor({
      codeField: 'code',
      dataField: 'data',
      successCode: 0,
    }),
  );
  client.addResponseInterceptor(
    authenticateResponseInterceptor({
      client,
      doReAuthenticate,
      doRefreshToken,
      enableRefreshToken: true,
      formatToken,
    }),
  );
  client.addResponseInterceptor(
    errorMessageResponseInterceptor({
      onError: (message, error: any) => {
        const responseData = error?.response?.data ?? {};
        const responseMessage = responseData?.error ?? responseData?.message;
        showToast({ message: responseMessage || message, type: 'fail' });
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
