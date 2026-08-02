<script setup lang="ts">
import { useI18n } from 'vue-i18n';

import LanguageSelector from '#/components/language/index.vue';

const { t } = useI18n();
</script>

<template>
  <main class="page home-page">
    <header class="home-page__header">
      <div class="home-page__toolbar">
        <LanguageSelector />
      </div>
      <h1 class="page__heading">{{ t('home.title') }}</h1>
      <p class="page__subtitle">{{ t('home.subtitle') }}</p>
    </header>

    <nav class="page__actions">
      <van-button block icon="contact-o" to="/account" type="primary">
        {{ t('home.account') }}
      </van-button>
      <van-button block icon="user-o" plain to="/login" type="primary">
        {{ t('home.login') }}
      </van-button>
    </nav>
  </main>
</template>

<style scoped src="../styles/page.css"></style>

<style scoped>
.home-page {
  padding: 24px 20px max(32px, env(safe-area-inset-bottom));
}

.home-page__header {
  padding-top: clamp(32px, 10vh, 88px);
}

.home-page__toolbar {
  display: flex;
  justify-content: flex-end;
  min-height: 32px;
  margin-bottom: 32px;
}
</style>
