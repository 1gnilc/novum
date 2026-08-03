import type { Customer } from '#/api/core';

import { computed, ref } from 'vue';

import { trimToNull } from '@vben/utils/shared';

import { defineStore } from 'pinia';

import {
  getCustomerUserInfo,
  login as loginCustomer,
  logout as logoutCustomer,
} from '#/api/core';
import { router } from '#/router';

interface LoginParams {
  password: string;
  username: string;
}

type Token = null | string;
type UserInfo = Customer;

export const useAuthStore = defineStore(
  'auth',
  () => {
    const accessToken = ref<Token>(null);
    const refreshToken = ref<Token>(null);
    const userInfo = ref<null | UserInfo>(null);
    const loginLoading = ref(false);
    const authenticated = computed(() => Boolean(accessToken.value));

    async function login(params: LoginParams) {
      try {
        loginLoading.value = true;
        const credentials = trimToNull({ ...params });
        const session = await loginCustomer(
          credentials.username,
          credentials.password,
        );
        setAccessToken(session.accessToken);
        setRefreshToken(session.refreshToken);
        try {
          await getUserInfo();
        } catch (error) {
          $reset();
          throw error;
        }
      } finally {
        loginLoading.value = false;
      }
      return { userInfo: userInfo.value };
    }

    async function getUserInfo() {
      userInfo.value = await getCustomerUserInfo();
      return userInfo.value;
    }

    async function logout(redirect = true) {
      const fullPath = router.currentRoute.value.fullPath;
      try {
        if (refreshToken.value) {
          await logoutCustomer(refreshToken.value);
        }
      } catch {
        // Local session cleanup must not depend on the remote logout result.
      }
      $reset();
      await router.replace({
        path: '/login',
        query: redirect ? { redirect: encodeURIComponent(fullPath) } : {},
      });
    }

    function setAccessToken(token: Token) {
      accessToken.value = token;
    }

    function setRefreshToken(token: Token) {
      refreshToken.value = token;
    }

    function $reset() {
      setAccessToken(null);
      setRefreshToken(null);
      userInfo.value = null;
      loginLoading.value = false;
    }

    return {
      $reset,
      accessToken,
      authenticated,
      getUserInfo,
      login,
      loginLoading,
      logout,
      refreshToken,
      setAccessToken,
      setRefreshToken,
      userInfo,
    };
  },
  {
    persist: {
      pick: ['accessToken', 'refreshToken'],
    },
  },
);
