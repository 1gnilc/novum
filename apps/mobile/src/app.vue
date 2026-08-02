<script setup lang="ts">
import { computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';

import { useTitle } from '@vueuse/core';
import dayjs from 'dayjs';
import { Locale } from 'vant';
import enUS from 'vant/es/locale/lang/en-US';
import zhCN from 'vant/es/locale/lang/zh-CN';

import GlobalLayout from '#/layouts/global-layout.vue';
import { getLocale } from '#/locales';

import 'dayjs/locale/en';
import 'dayjs/locale/zh-cn';

const { locale, t } = useI18n();
const route = useRoute();
const appTitle = import.meta.env.VITE_APP_TITLE || 'Novum Mobile';
const dayjsLocales = { 'en-US': 'en', 'zh-CN': 'zh-cn' } as const;
const vantLocales = { 'en-US': enUS, 'zh-CN': zhCN } as const;

useTitle(
  computed(() => {
    const key = route.meta.title;
    return key ? `${t(key)} - ${appTitle}` : appTitle;
  }),
);

watch(
  locale,
  () => {
    const current = getLocale();
    document.documentElement.lang = current;
    Locale.use(current, vantLocales[current]);
    dayjs.locale(dayjsLocales[current]);
  },
  { immediate: true },
);
</script>

<template>
  <GlobalLayout />
</template>
