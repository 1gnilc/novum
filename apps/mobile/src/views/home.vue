<script setup lang="ts">
import type { Theme } from '#/stores';

import { useI18n } from 'vue-i18n';

import bannerImage from '#/assets/images/banner-brand.png';
import NavBar from '#/components/nav-bar/index.vue';
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
    <NavBar fixed placeholder locale :title="t('home.title')" />
    <header class="home-page__header">
      <img class="home-page__banner" :src="bannerImage" alt="Novum" />
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

    <nav class="home-page__actions">
      <van-button block icon="user-o" plain to="/login" type="primary">
        {{ t('home.login') }}
      </van-button>
    </nav>
  </main>
</template>

<style scoped src="../styles/page.css"></style>

<style scoped lang="scss">
.home-page {
  padding-bottom: 24px;

  &__header {
    aspect-ratio: 1029 / 480;
    overflow: hidden;
    background: var(--color-surface-deep);
  }

  &__banner {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &__theme {
    padding: 24px 20px 0;
  }

  &__section-heading {
    margin: 0 0 14px;
    font-size: 14px;
    font-weight: 600;
    line-height: 20px;
    color: var(--van-text-color-2);
  }

  &__theme-options {
    display: flex;
    flex-wrap: wrap;
    gap: 12px 20px;
    min-height: 24px;
  }

  &__actions {
    padding: 24px 20px 0;
  }
}
</style>
