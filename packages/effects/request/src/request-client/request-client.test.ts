import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  authenticateResponseInterceptor,
  errorMessageResponseInterceptor,
} from './preset-interceptors';
import { RequestClient } from './request-client';

describe('requestClient', () => {
  let mock: MockAdapter;
  let requestClient: RequestClient;

  beforeEach(() => {
    mock = new MockAdapter(axios);
    requestClient = new RequestClient();
  });

  afterEach(() => {
    mock.reset();
  });

  it('should successfully make a GET request', async () => {
    mock.onGet('test/url').reply(200, { data: 'response' });

    const response = await requestClient.get('test/url');

    expect(response.data).toEqual({ data: 'response' });
  });

  it('should successfully make a POST request', async () => {
    const postData = { key: 'value' };
    const mockData = { data: 'response' };
    mock.onPost('/test/post', postData).reply(200, mockData);
    const response = await requestClient.post('/test/post', postData);
    expect(response.data).toEqual(mockData);
  });

  it('should successfully make a PUT request', async () => {
    const putData = { key: 'updatedValue' };
    const mockData = { data: 'updated response' };
    mock.onPut('/test/put', putData).reply(200, mockData);
    const response = await requestClient.put('/test/put', putData);
    expect(response.data).toEqual(mockData);
  });

  it('should successfully make a DELETE request', async () => {
    const mockData = { data: 'delete response' };
    mock.onDelete('/test/delete').reply(200, mockData);
    const response = await requestClient.delete('/test/delete');
    expect(response.data).toEqual(mockData);
  });

  it('should handle network errors', async () => {
    mock.onGet('/test/error').networkError();
    await expect(requestClient.get('/test/error')).rejects.toMatchObject({
      isAxiosError: true,
      message: 'Network Error',
    });
  });

  it('should handle timeout', async () => {
    mock.onGet('/test/timeout').timeout();
    await expect(requestClient.get('/test/timeout')).rejects.toMatchObject({
      isAxiosError: true,
      code: 'ECONNABORTED',
    });
  });

  it('should preserve HTTP error metadata and response data', async () => {
    const responseData = {
      code: 20_002,
      data: null,
      error: 'Unauthenticated.',
      message: 'Unauthenticated.',
    };
    mock.onGet('/test/unauthorized').reply(401, responseData);

    await expect(requestClient.get('/test/unauthorized')).rejects.toMatchObject(
      {
        isAxiosError: true,
        response: {
          data: responseData,
          status: 401,
        },
      },
    );
  });

  it('should expose a failed refresh response to the business error handler', async () => {
    const businessClient = new RequestClient();
    const baseClient = new RequestClient();
    const refreshResponseData = {
      code: 20_002,
      data: null,
      error: 'Unauthenticated.',
      message: 'Unauthenticated.',
    };
    const doReAuthenticate = vi.fn();
    const showError = vi.fn();

    businessClient.addResponseInterceptor(
      authenticateResponseInterceptor({
        client: businessClient,
        doReAuthenticate,
        doRefreshToken: async () => {
          const response = await baseClient.get<{
            data: { data: { accessToken: string } };
          }>('/test/refresh');
          return response.data.data.accessToken;
        },
        enableRefreshToken: true,
        formatToken: (token) => `Bearer ${token}`,
      }),
    );
    businessClient.addResponseInterceptor(
      errorMessageResponseInterceptor((fallbackMessage, error) => {
        const responseData = error?.response?.data ?? {};
        showError(
          responseData.error ?? responseData.message ?? fallbackMessage,
        );
      }),
    );
    mock.onGet('/test/page').reply(401, {
      code: 20_002,
      data: null,
      error: 'The access token is invalid or has expired.',
      message: 'The access token is invalid or has expired.',
    });
    mock.onGet('/test/refresh').reply(401, refreshResponseData);

    await expect(businessClient.get('/test/page')).rejects.toMatchObject({
      isAxiosError: true,
      response: {
        data: refreshResponseData,
        status: 401,
      },
    });
    expect(doReAuthenticate).toHaveBeenCalledOnce();
    expect(showError).toHaveBeenCalledOnce();
    expect(showError).toHaveBeenCalledWith('Unauthenticated.');
  });

  it('should successfully upload a file', async () => {
    const fileData = new Blob(['file contents'], { type: 'text/plain' });

    mock.onPost('/test/upload').reply((config) => {
      return config.data instanceof FormData && config.data.has('file')
        ? [200, { data: 'file uploaded' }]
        : [400, { error: 'Bad Request' }];
    });

    const response = await requestClient.upload('/test/upload', {
      file: fileData,
    });
    expect(response.data).toEqual({ data: 'file uploaded' });
  });

  it('should successfully download a file as a blob', async () => {
    const mockFileContent = new Blob(['mock file content'], {
      type: 'text/plain',
    });

    mock.onGet('/test/download').reply(200, mockFileContent);

    const res = await requestClient.download<any>('/test/download');

    expect(res.data).toBeInstanceOf(Blob);
  });
});
