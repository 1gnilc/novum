import type { ConfigProviderTheme } from 'vant';

import { computed, ref } from 'vue';

import { useEventListener } from '@vueuse/core';
import { defineStore } from 'pinia';

import { getSystemTheme, SYSTEM_THEME_QUERIES } from '#/utils/theme';

const DEFAULT_THEME: Theme = 'system';
const FALLBACK_THEME: ConfigProviderTheme = 'dark';

export type Theme = 'system' | ConfigProviderTheme;

const useThemeStore = defineStore(
  'theme',
  () => {
    const theme = ref<Theme>(DEFAULT_THEME);
    const systemTheme = ref(getSystemTheme());
    const resolvedTheme = computed<ConfigProviderTheme>(() => {
      if (theme.value === 'system') {
        return systemTheme.value ?? FALLBACK_THEME;
      }

      return theme.value;
    });

    if (
      typeof window !== 'undefined' &&
      typeof window.matchMedia === 'function'
    ) {
      for (const query of Object.values(SYSTEM_THEME_QUERIES)) {
        useEventListener(window.matchMedia(query), 'change', () => {
          systemTheme.value = getSystemTheme();
        });
      }
    }

    return { resolvedTheme, theme };
  },
  {
    persist: {
      pick: ['theme'],
    },
  },
);

export { useThemeStore };
