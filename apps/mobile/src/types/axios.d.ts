import 'axios';

interface RequestAuthOptions {
  required?: boolean;
}

declare module 'axios' {
  interface AxiosRequestConfig {
    requestAuth?: RequestAuthOptions;
  }

  interface InternalAxiosRequestConfig {
    requestAuth?: RequestAuthOptions;
  }
}
