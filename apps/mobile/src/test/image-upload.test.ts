import { beforeEach, describe, expect, it, vi } from 'vitest';

import { IMAGE_MAX_SIZE, uploadImage } from '#/composables/use-image-upload';

const api = vi.hoisted(() => ({
  finalizeImageUpload: vi.fn(),
  presignImageUpload: vi.fn(),
}));

vi.mock('#/api/core', () => api);

describe('mobile image upload', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal('fetch', vi.fn());
  });

  it('rejects unsupported and oversized files before presigning', async () => {
    await expect(
      uploadImage(new File(['svg'], 'image.svg', { type: 'image/svg+xml' })),
    ).rejects.toThrow('Only JPEG, PNG, and WebP images are supported.');
    await expect(
      uploadImage(
        new File([new Uint8Array(IMAGE_MAX_SIZE + 1)], 'large.webp', {
          type: 'image/webp',
        }),
      ),
    ).rejects.toThrow('Image size must not exceed 3 MiB.');
    expect(api.presignImageUpload).not.toHaveBeenCalled();
  });

  it('presigns, uploads, and finalizes in order', async () => {
    const file = new File(['image'], 'image.webp', { type: 'image/webp' });
    api.presignImageUpload.mockResolvedValue({
      headers: { 'Content-Type': 'image/webp' },
      method: 'PUT',
      objectKey: 'images/2026/08/05/file.webp',
      uploadUrl: 'https://upload.example.test/file',
    });
    api.finalizeImageUpload.mockResolvedValue({
      objectKey: 'images/2026/08/05/file.webp',
    });
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 200 }));

    await expect(uploadImage(file)).resolves.toMatchObject({
      objectKey: 'images/2026/08/05/file.webp',
    });
    expect(api.presignImageUpload).toHaveBeenCalledWith({
      contentLength: file.size,
      contentType: 'image/webp',
    });
    expect(fetch).toHaveBeenCalledOnce();
    expect(api.finalizeImageUpload).toHaveBeenCalledWith(
      'images/2026/08/05/file.webp',
    );
  });
});
