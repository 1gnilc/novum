<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';

import { useAuthStore } from '#/stores';

const auth = useAuthStore();
const loading = ref(false);
const router = useRouter();
const { t } = useI18n();

onMounted(async () => {
  if (!auth.authenticated || auth.userInfo) {
    return;
  }
  loading.value = true;
  try {
    await auth.getUserInfo();
  } catch {
    // Request errors are presented by the application request client.
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <main class="page">
    <van-nav-bar
      :title="t('account.title')"
      left-arrow
      @click-left="router.push('/')"
    />

    <section class="page__content">
      <van-skeleton v-if="loading" :row="3" title />
      <van-cell-group v-else-if="auth.userInfo" inset>
        <van-cell
          :title="t('account.username')"
          :value="auth.userInfo.username"
        />
        <van-cell
          :title="t('account.nickname')"
          :value="auth.userInfo.nickname"
        />
        <van-cell
          :title="t('account.roles')"
          :value="auth.userInfo.roleCodes.join(', ')"
        />
      </van-cell-group>
      <van-empty
        v-else
        :description="
          auth.authenticated ? t('account.unavailable') : t('account.anonymous')
        "
      />

      <div v-if="auth.authenticated" class="page__actions">
        <van-button block plain type="danger" @click="auth.logout()">
          {{ t('account.logout') }}
        </van-button>
      </div>
    </section>
  </main>
</template>

<style scoped src="../styles/page.css"></style>
