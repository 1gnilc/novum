<script setup lang="ts">
import { useI18n } from 'vue-i18n';

import NavBar from '#/components/nav-bar/index.vue';

const { t } = useI18n();
</script>

<template>
  <main class="page section-page">
    <NavBar locale :title="t('fund.title')" />
    <section class="section-page__content">
      <van-icon class="section-page__icon" name="balance-o" />
      <h1 class="section-page__title">{{ t('fund.title') }}</h1>
    </section>
  </main>
</template>

<style scoped src="../styles/page.css"></style>
<style scoped lang="scss" src="../styles/section-page.scss"></style>
