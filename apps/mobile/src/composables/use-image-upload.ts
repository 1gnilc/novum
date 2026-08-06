import type { Image } from '#/api/core';

import { finalizeImageUpload, presignImageUpload } from '#/api/core';

export const IMAGE_MAX_SIZE = 3 * 1024 * 1024;
export const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const;

export function validateImage(file: File) {
  if (!IMAGE_TYPES.includes(file.type as (typeof IMAGE_TYPES)[number])) {
    throw new Error('Only JPEG, PNG, and WebP images are supported.');
  }
  if (file.size <= 0) {
    throw new Error('Image size must be greater than zero.');
  }
  if (file.size > IMAGE_MAX_SIZE) {
    throw new Error('Image size must not exceed 3 MiB.');
  }
}

export async function uploadImage(file: File): Promise<Image> {
  validateImage(file);
  const signature = await presignImageUpload({
    contentLength: file.size,
    contentType: file.type,
  });
  const response = await fetch(signature.uploadUrl, {
    body: file,
    headers: signature.headers,
    method: signature.method,
  });
  if (!response.ok) {
    throw new Error(`Image upload failed with status ${response.status}.`);
  }
  return finalizeImageUpload(signature.objectKey);
}

export function useImageUpload() {
  return { uploadImage, validateImage };
}
