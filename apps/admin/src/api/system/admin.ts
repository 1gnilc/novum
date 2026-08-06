import type { PageParams, PageResult } from '#/api/types';

import { isEmpty } from '@vben/utils';

import { requestClient } from '#/api/request';

export namespace AdminApi {
  export interface Admin {
    avatar?: string;
    avatarUrl?: string;
    createTime: string;
    desc?: string;
    homePath: string;
    id: string;
    nickname: string;
    roleCodes: string[];
    status?: boolean;
    userId: string;
    username: string;
  }
}

export async function getAdminPage(
  params?: PageParams &
    Partial<Pick<AdminApi.Admin, 'nickname' | 'status' | 'username'>>,
) {
  return requestClient.post<PageResult<AdminApi.Admin>>(
    '/sys/admin/page',
    params,
  );
}

export async function createAdmin(
  data: Omit<
    AdminApi.Admin,
    'avatarUrl' | 'createTime' | 'id' | 'roleCodes' | 'userId'
  > & {
    password: string;
  },
) {
  return requestClient.post<null>('/sys/admin/create', data);
}

export async function updateAdmin(
  data: Partial<
    Omit<
      AdminApi.Admin,
      'avatarUrl' | 'createTime' | 'id' | 'roleCodes' | 'userId'
    >
  > &
    Pick<AdminApi.Admin, 'id'> & { password?: null | string },
) {
  const { password, ...profile } = data;
  const requestData = isEmpty(password?.trim()) ? profile : data;

  return requestClient.post<null>('/sys/admin/update', requestData);
}

export async function saveAdminRoles(id: string, roleCodes: string[]) {
  return requestClient.post<null>('/sys/admin/roles/save', {
    id,
    roleCodes,
  });
}

export async function removeAdmin(id: string) {
  return requestClient.post<null>(
    `/sys/admin/remove/${encodeURIComponent(id)}`,
  );
}
