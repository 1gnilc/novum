<script setup lang="ts">
import { useRoute } from 'vue-router';

import LoginRequired from '#/components/authentication/login-required.vue';
import Tabbar from '#/components/tabbar/index.vue';

const route = useRoute();
</script>

<template>
  <div
    class="mobile-layout"
    :class="{ 'mobile-layout--tabbar': route.meta.tabbar }"
  >
    <RouterView />
    <LoginRequired />
    <Tabbar v-if="route.meta.tabbar" />
  </div>
</template>

<style scoped lang="scss">
.mobile-layout {
  min-height: 100vh;
  min-height: 100dvh;

  &--tabbar {
    padding-bottom: calc(
      var(--mobile-tabbar-height) + env(safe-area-inset-bottom)
    );
  }
}
</style>
