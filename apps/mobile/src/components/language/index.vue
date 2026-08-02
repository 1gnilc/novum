<script setup lang="ts">
import type { ActionSheetAction } from 'vant';

import type { AppLocale } from '#/locales';

import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';

import { preferences, updatePreferences } from '@vben/preferences';

import { loadLocaleMessages, SUPPORTED_LOCALES } from '#/locales';

type LocaleAction = ActionSheetAction & { value: AppLocale };

defineOptions({ name: 'LanguageSelector' });

const { t } = useI18n();
const show = ref(false);
const localeNames: Record<AppLocale, string> = {
  'en-US': 'language.enUS',
  'zh-CN': 'language.zhCN',
};

const currentName = computed(() => t(localeNames[preferences.app.locale]));
const actions = computed<LocaleAction[]>(() =>
  SUPPORTED_LOCALES.map((locale) => ({
    color: locale === preferences.app.locale ? '#1989fa' : undefined,
    name: t(localeNames[locale]),
    subname:
      locale === preferences.app.locale ? t('language.current') : undefined,
    value: locale,
  })),
);

async function select(action: LocaleAction) {
  if (!SUPPORTED_LOCALES.includes(action.value)) {
    return;
  }
  updatePreferences({ app: { locale: action.value } });
  await loadLocaleMessages(action.value);
  show.value = false;
}
</script>

<template>
  <van-button
    icon="exchange"
    plain
    size="small"
    type="primary"
    @click="show = true"
  >
    {{ currentName }}
  </van-button>
  <van-action-sheet
    v-model:show="show"
    :actions="actions"
    :cancel-text="t('auth.cancel')"
    :title="t('language.label')"
    teleport="body"
    @select="select"
  />
</template>
