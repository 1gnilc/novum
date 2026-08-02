import { baseRequestClient, requestClient, requestConfig } from './request';

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
  return baseRequestClient.post<CustomerSession>(
    '/customer/login',
    { password, username },
    { responseReturn: 'data' },
  );
}

export async function logout(refreshToken: string) {
  await baseRequestClient.post('/customer/logout', undefined, {
    headers: { [REFRESH_TOKEN_HEADER]: refreshToken },
    responseReturn: 'data',
  });
}

export function getUserInfo() {
  return requestClient.get<CustomerInfo>(
    '/customer/user-info',
    requestConfig({ auth: { required: true } }),
  );
}
