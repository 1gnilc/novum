import type {
  AxiosResponse,
  HttpResponse,
  RequestClientConfig,
  RequestClientOptions,
  RequestErrorType,
} from '@vben/request';

import {
  authenticateResponseInterceptor,
  defaultResponseInterceptor,
  errorMessageResponseInterceptor,
  RequestClient,
} from '@vben/request';

import { AuthenticationRequiredError } from '#/errors/authentication-required';
import { getLocale, translate } from '#/locales';
import { useAuthStore } from '#/stores';

const requestErrorMessages: Record<RequestErrorType, string> = {
  'bad-request': 'request.badRequest',
  forbidden: 'request.forbidden',
  'internal-server-error': 'request.internalServerError',
  'network-error': 'request.networkError',
  'not-found': 'request.notFound',
  'request-timeout': 'request.requestTimeout',
  unauthorized: 'request.unauthorized',
};

interface TokenPair {
  accessToken: string;
  refreshToken: string;
}

interface RequestAuthOptions {
  required?: boolean;
}

interface RequestError {
  response?: {
    data?: { error?: string; message?: string };
    status?: number;
  };
}

type MobileRequestConfig = Omit<RequestClientConfig, 'auth' | 'requestAuth'> & {
  auth?: RequestAuthOptions;
};

const apiURL = import.meta.env.VITE_APP_API_URL || '/api';
const refreshTokenHeader = 'X-Refresh-Token';

export const baseRequestClient = new RequestClient({ baseURL: apiURL });

export function requestConfig(config: MobileRequestConfig) {
  const { auth, ...request } = config;
  return { ...request, requestAuth: auth };
}

function showRequestError(message: string, error: unknown) {
  const data = (error as RequestError)?.response?.data;
  showToast({
    message: data?.error || data?.message || message,
    type: 'fail',
  });
}

async function refreshSession(refreshToken: string) {
  const response = await baseRequestClient.post<
    AxiosResponse<HttpResponse<TokenPair>>
  >('/customer/refresh', undefined, {
    headers: { [refreshTokenHeader]: refreshToken },
  });
  return response.data.data;
}

export function createRequestClient(
  baseURL: string,
  options?: RequestClientOptions,
) {
  const client = new RequestClient({ ...options, baseURL });

  async function doReAuthenticate() {
    useAuthStore().$reset();
  }

  async function doRefreshToken() {
    const auth = useAuthStore();
    if (!auth.refreshToken) {
      throw new Error('Refresh token is missing.');
    }
    const session = await refreshSession(auth.refreshToken);
    auth.$patch(session);
    return session.accessToken;
  }

  function formatToken(token: null | string) {
    return token ? `Bearer ${token}` : null;
  }

  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const auth = useAuthStore();
      if (config.requestAuth?.required && !auth.accessToken) {
        throw new AuthenticationRequiredError();
      }
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
      onError: (message, error) => {
        if (error instanceof AuthenticationRequiredError) {
          return;
        }
        showRequestError(message, error);
      },
      resolveMessage: (type) => translate(requestErrorMessages[type]),
    }),
  );

  return client;
}

baseRequestClient.addRequestInterceptor({
  fulfilled: async (config) => {
    config.headers['Accept-Language'] = getLocale();
    return config;
  },
});
baseRequestClient.addResponseInterceptor(
  defaultResponseInterceptor({
    codeField: 'code',
    dataField: 'data',
    successCode: 0,
  }),
);
baseRequestClient.addResponseInterceptor(
  errorMessageResponseInterceptor({
    onError: (message, error) => {
      if ((error as RequestError)?.response?.status === 401) {
        return;
      }
      showRequestError(message, error);
    },
    resolveMessage: (type) => translate(requestErrorMessages[type]),
  }),
);

export const requestClient = createRequestClient(apiURL, {
  responseReturn: 'data',
});
