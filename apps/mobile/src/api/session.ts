import type { AxiosResponse, HttpResponse } from '@vben/request';

import { baseRequestClient, requestClient } from './request';

export interface CustomerSession {
  accessToken: string;
  refreshToken: string;
}

export interface CustomerInfo {
  avatar?: string;
  createTime: string;
  id: string;
  nickname: string;
  roleCodes: string[];
  userId: string;
  username: string;
}

const REFRESH_TOKEN_HEADER = 'X-Refresh-Token';

export function login(username: string, password: string) {
  return requestClient.post<CustomerSession>('/customer/login', {
    password,
    username,
  });
}

export async function refresh(refreshToken: string) {
  const response = await baseRequestClient.post<
    AxiosResponse<HttpResponse<CustomerSession>>
  >('/customer/refresh', undefined, {
    headers: { [REFRESH_TOKEN_HEADER]: refreshToken },
  });
  return response.data.data;
}

export async function logout(refreshToken: string) {
  await baseRequestClient.post<AxiosResponse<HttpResponse<null>>>(
    '/customer/logout',
    undefined,
    { headers: { [REFRESH_TOKEN_HEADER]: refreshToken } },
  );
}

export function getUserInfo() {
  return requestClient.get<CustomerInfo>('/customer/user-info', {
    requestAuth: { required: true },
  });
}
