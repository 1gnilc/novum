<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';

import NavBar from '#/components/nav-bar/index.vue';
import { useAuthStore } from '#/stores';

const auth = useAuthStore();
const loading = ref(false);
const { t } = useI18n();

onMounted(async () => {
  if (!auth.authenticated || auth.userInfo) return;
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
  <main class="page my-page">
    <NavBar locale :title="t('my.title')" />

    <section class="page__content">
      <van-skeleton v-if="loading" :row="3" title />
      <div v-else-if="auth.userInfo" class="my-page__profile">
        <van-image
          v-if="auth.userInfo.avatarUrl"
          class="my-page__avatar"
          fit="cover"
          round
          :src="auth.userInfo.avatarUrl"
        />
        <van-icon v-else class="my-page__avatar-placeholder" name="contact" />
        <strong class="my-page__nickname">{{ auth.userInfo.nickname }}</strong>
        <span class="my-page__username">{{ auth.userInfo.username }}</span>
      </div>
      <van-empty
        v-else
        :description="
          auth.authenticated ? t('my.unavailable') : t('my.anonymous')
        "
      />

      <van-cell-group v-if="auth.userInfo" class="my-page__details" inset>
        <van-cell :title="t('my.username')" :value="auth.userInfo.username" />
        <van-cell :title="t('my.nickname')" :value="auth.userInfo.nickname" />
        <van-cell
          :title="t('my.roles')"
          :value="auth.userInfo.roleCodes.join(', ')"
        />
      </van-cell-group>

      <div v-if="auth.authenticated" class="page__actions">
        <van-button block plain type="danger" @click="auth.logout()">
          {{ t('my.logout') }}
        </van-button>
      </div>
    </section>
  </main>
</template>

<style scoped src="../styles/page.css"></style>

<style scoped lang="scss">
.my-page {
  &__profile {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 16px 0 28px;
  }

  &__avatar,
  &__avatar-placeholder {
    width: 76px;
    height: 76px;
  }

  &__avatar-placeholder {
    font-size: 76px;
    color: var(--color-primary);
  }

  &__nickname {
    margin-top: 14px;
    font-size: 20px;
    line-height: 1.3;
  }

  &__username {
    margin-top: 4px;
    font-size: 14px;
    color: var(--van-text-color-2);
  }

  &__details {
    display: block;
  }
}
</style>
