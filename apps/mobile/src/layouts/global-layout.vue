<script setup lang="ts">
import LoginRequiredActionSheet from '#/components/authentication/login-required-action-sheet.vue';
</script>

<template>
  <RouterView />
  <LoginRequiredActionSheet />
</template>
