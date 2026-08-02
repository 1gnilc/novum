<script setup lang="ts">
import type { ActionSheetAction } from 'vant';

import { computed, ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import { useAuthStore } from '#/stores';

type LoginAction = ActionSheetAction & { value: 'login' };

defineOptions({ name: 'LoginRequired' });

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const dismissed = ref(false);

const actions = computed<LoginAction[]>(() => [
  { name: t('auth.openLogin'), value: 'login' },
]);
const show = computed({
  get: () =>
    Boolean(route.meta.requiresAuth) && !auth.authenticated && !dismissed.value,
  set: (value: boolean) => {
    if (!value) {
      dismissed.value = true;
    }
  },
});

watch(
  () => route.fullPath,
  () => {
    dismissed.value = false;
  },
);
watch(
  () => auth.authenticated,
  (authenticated) => {
    if (authenticated) {
      dismissed.value = false;
    }
  },
);

async function select(action: LoginAction) {
  if (action.value !== 'login') {
    return;
  }
  await router.push({
    path: '/login',
    query: { redirect: encodeURIComponent(route.fullPath) },
  });
}
</script>

<template>
  <van-action-sheet
    v-model:show="show"
    :actions="actions"
    :cancel-text="t('auth.cancel')"
    :close-on-click-overlay="false"
    :description="t('auth.loginRequiredDescription')"
    :safe-area-inset-bottom="true"
    :title="t('auth.loginRequired')"
    teleport="body"
    @cancel="dismissed = true"
    @select="select"
  />
</template>
