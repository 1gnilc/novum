<script setup lang="ts">
import type { ActionSheetAction } from 'vant';

import type { AppLocale } from '#/locales/locale';

import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';

import { loadLocaleMessages } from '#/locales';
import { getLocaleItem, LOCALE_ITEMS } from '#/locales/locale';
import { usePreferences } from '#/stores';

type LocaleAction = ActionSheetAction & { value: AppLocale };

defineOptions({ name: 'Locale' });

const { t } = useI18n();
const preferences = usePreferences();
const show = ref(false);
const current = computed(() => getLocaleItem(preferences.locale));
const actions = computed<LocaleAction[]>(() =>
  LOCALE_ITEMS.map((item) => ({
    color:
      item.locale === preferences.locale ? 'var(--color-primary)' : undefined,
    name: item.name,
    subname:
      item.locale === preferences.locale ? t('language.current') : undefined,
    value: item.locale,
  })),
);

async function select(action: LocaleAction) {
  preferences.setLocale(action.value);
  await loadLocaleMessages(action.value);
  show.value = false;
}
</script>

<template>
  <button
    class="locale-trigger"
    type="button"
    :aria-label="current?.name"
    @click="show = true"
  >
    <img v-if="current" :src="current.flags" :alt="current.name" />
  </button>
  <van-action-sheet
    v-model:show="show"
    :actions="actions"
    :cancel-text="t('auth.cancel')"
    :title="t('language.label')"
    teleport="body"
    @select="select"
  />
</template>

<style scoped lang="scss">
.locale-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 5px;
  color: var(--van-text-color);
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: var(--radius-round);

  &:focus-visible {
    outline: 2px solid var(--color-focus);
    outline-offset: 2px;
  }

  img {
    width: 26px;
    height: 18px;
    object-fit: cover;
    border-radius: 3px;
  }
}
</style>
