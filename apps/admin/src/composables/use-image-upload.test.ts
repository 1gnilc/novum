import { beforeEach, describe, expect, it, vi } from 'vitest';

import { IMAGE_MAX_SIZE, uploadImage, validateImage } from './use-image-upload';

const api = vi.hoisted(() => ({
  finalizeImageUpload: vi.fn(),
  presignImageUpload: vi.fn(),
}));

vi.mock('#/api', () => api);

describe('admin image upload', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal('fetch', vi.fn());
  });

  it('rejects SVG and oversized files before requesting a signature', async () => {
    const svg = new File(['<svg />'], 'image.svg', { type: 'image/svg+xml' });
    const oversized = new File(
      [new Uint8Array(IMAGE_MAX_SIZE + 1)],
      'large.png',
      {
        type: 'image/png',
      },
    );

    expect(() => validateImage(svg)).toThrow(
      'Only JPEG, PNG, and WebP images are supported.',
    );
    await expect(uploadImage(oversized)).rejects.toThrow(
      'Image size must not exceed 3 MiB.',
    );
    expect(api.presignImageUpload).not.toHaveBeenCalled();
  });

  it('presigns, uploads directly, then finalizes in order', async () => {
    const file = new File(['png'], 'image.png', { type: 'image/png' });
    api.presignImageUpload.mockResolvedValue({
      expiresAt: '2026-08-05T04:40:00Z',
      headers: { 'Content-Type': 'image/png' },
      method: 'PUT',
      objectKey: 'images/2026/08/05/file.png',
      uploadUrl: 'https://upload.example.test/file',
    });
    api.finalizeImageUpload.mockResolvedValue({
      objectKey: 'images/2026/08/05/file.png',
    });
    vi.mocked(fetch).mockResolvedValue(new Response(null, { status: 200 }));

    await expect(uploadImage(file)).resolves.toEqual({
      objectKey: 'images/2026/08/05/file.png',
    });
    expect(api.presignImageUpload).toHaveBeenCalledWith({
      contentLength: file.size,
      contentType: 'image/png',
    });
    expect(fetch).toHaveBeenCalledWith(
      'https://upload.example.test/file',
      expect.objectContaining({ body: file, method: 'PUT' }),
    );
    expect(api.finalizeImageUpload).toHaveBeenCalledWith(
      'images/2026/08/05/file.png',
    );
  });
});
