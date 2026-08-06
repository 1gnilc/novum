import { requestClient } from '#/api/request';

export interface Image {
  contentLength: number;
  contentType: string;
  createTime: string;
  id: string;
  objectKey: string;
  status: 'PENDING' | 'READY';
  url: string;
}

export interface ImagePresign {
  expiresAt: string;
  headers: Record<string, string>;
  method: 'PUT';
  objectKey: string;
  uploadUrl: string;
}

export function presignImageUpload(data: {
  contentLength: number;
  contentType: string;
}) {
  return requestClient.post<ImagePresign>('/image/presign', data);
}

export function finalizeImageUpload(objectKey: string) {
  return requestClient.post<Image>('/image/finalize', { objectKey });
}
