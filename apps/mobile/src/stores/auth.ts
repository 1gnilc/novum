import type { CustomerInfo } from '#/api/session';

import { computed, ref } from 'vue';

import { trimToNull } from '@vben/utils/shared';

import { defineStore } from 'pinia';

import {
  getUserInfo as fetchUserInfo,
  login as loginCustomer,
  logout as logoutCustomer,
} from '#/api/session';
import { router } from '#/router';

interface LoginParams {
  password: string;
  username: string;
}

export const useAuthStore = defineStore(
  'auth',
  () => {
    const accessToken = ref<null | string>(null);
    const refreshToken = ref<null | string>(null);
    const userInfo = ref<CustomerInfo | null>(null);
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
        accessToken.value = session.accessToken;
        refreshToken.value = session.refreshToken;
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
      const info = await fetchUserInfo();
      userInfo.value = info;
      return info;
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

    function $reset() {
      accessToken.value = null;
      refreshToken.value = null;
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
      userInfo,
    };
  },
  {
    persist: {
      pick: ['accessToken', 'refreshToken'],
    },
  },
);
