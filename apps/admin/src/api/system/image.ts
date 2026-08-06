import type { PageParams, PageResult } from '#/api/types';

import { requestClient } from '#/api/request';

export namespace ImageApi {
  export interface Image {
    contentLength: number;
    contentType: string;
    createTime: string;
    id: string;
    objectKey: string;
    status: 'PENDING' | 'READY';
    url: string;
  }

  export interface Presign {
    expiresAt: string;
    headers: Record<string, string>;
    method: 'PUT';
    objectKey: string;
    uploadUrl: string;
  }
}

export function presignImageUpload(
  data: Pick<ImageApi.Image, 'contentLength' | 'contentType'>,
) {
  return requestClient.post<ImageApi.Presign>('/image/presign', data);
}

export function finalizeImageUpload(objectKey: string) {
  return requestClient.post<ImageApi.Image>('/image/finalize', { objectKey });
}

export function getImagePage(params?: PageParams) {
  return requestClient.post<PageResult<ImageApi.Image>>('/image/page', params);
}

export function removeImage(id: string) {
  return requestClient.post<null>(`/image/remove/${encodeURIComponent(id)}`);
}
