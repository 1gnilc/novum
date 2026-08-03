<script setup lang="ts">
import type { Theme } from '#/stores';

import { useI18n } from 'vue-i18n';

import LanguageSelector from '#/components/language/index.vue';
import { useThemeStore } from '#/stores';

interface ThemeOption {
  label: string;
  value: Theme;
}

const { t } = useI18n();
const themeStore = useThemeStore();
const themeOptions: ThemeOption[] = [
  { label: 'theme.light', value: 'light' },
  { label: 'theme.dark', value: 'dark' },
  { label: 'theme.system', value: 'system' },
];
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

    <section class="home-page__theme" aria-labelledby="theme-heading">
      <h2 id="theme-heading" class="home-page__section-heading">
        {{ t('theme.label') }}
      </h2>
      <van-radio-group
        v-model="themeStore.theme"
        class="home-page__theme-options"
        direction="horizontal"
      >
        <van-radio
          v-for="option in themeOptions"
          :key="option.value"
          :name="option.value"
        >
          {{ t(option.label) }}
        </van-radio>
      </van-radio-group>
    </section>

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

.home-page__theme {
  margin-top: 32px;
}

.home-page__section-heading {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 600;
  line-height: 20px;
  color: var(--van-text-color-2);
}

.home-page__theme-options {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 20px;
  min-height: 24px;
}
</style>
