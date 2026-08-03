<script setup lang="ts">
import { useThemeStore } from '#/stores';
import { themeVars } from '#/styles/theme';

defineOptions({ name: 'App' });

const themeStore = useThemeStore();
</script>

<template>
  <van-config-provider
    :theme="themeStore.resolvedTheme"
    :theme-vars="themeVars"
    theme-vars-scope="global"
  >
    <RouterView />
  </van-config-provider>
</template>
