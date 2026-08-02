<script setup lang="ts">
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
</script>

<template>
  <main class="page page__empty">
    <van-empty image="error" :description="t('notFound.title')">
      <van-button round to="/" type="primary">
        {{ t('notFound.back') }}
      </van-button>
    </van-empty>
  </main>
</template>

<style scoped src="../styles/page.css"></style>
