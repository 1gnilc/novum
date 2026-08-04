<script setup lang="ts">
import { useThemeStore } from '#/stores';

defineOptions({ name: 'App' });

const themeStore = useThemeStore();
</script>

<template>
  <van-config-provider :theme="themeStore.resolvedTheme">
    <RouterView />
  </van-config-provider>
</template>
