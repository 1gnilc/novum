<script setup lang="ts">
import LoginRequired from '#/components/authentication/login-required.vue';
</script>

<template>
  <RouterView />
  <LoginRequired />
</template>
