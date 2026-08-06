<script setup lang="ts">
import { reactive } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

import Locale from '#/components/locale/index.vue';
import NavBar from '#/components/nav-bar/index.vue';
import { resolveRedirect } from '#/router/redirect';
import { useAuthStore } from '#/stores';

interface LoginForm {
  password: string;
  username: string;
}

const auth = useAuthStore();
const form = reactive<LoginForm>({ password: '', username: '' });
const route = useRoute();
const router = useRouter();
const { t } = useI18n();

async function submit() {
  try {
    await auth.login(form);
    showToast({ message: t('login.success'), type: 'success' });
    await router.replace(resolveRedirect(route.query.redirect));
  } catch {
    // Request errors are presented by the application request client.
  }
}
</script>

<template>
  <main class="page">
    <NavBar :title="t('login.title')" left-arrow>
      <template #right>
        <Locale />
      </template>
    </NavBar>

    <section class="page__content">
      <van-form class="page__form" validate-first @submit="submit">
        <van-cell-group inset>
          <van-field
            v-model="form.username"
            autocomplete="username"
            clearable
            :label="t('login.username')"
            name="username"
            :placeholder="t('login.username')"
            :rules="[{ required: true, message: t('login.usernameRequired') }]"
          />
          <van-field
            v-model="form.password"
            autocomplete="current-password"
            clearable
            :label="t('login.password')"
            name="password"
            :placeholder="t('login.password')"
            :rules="[{ required: true, message: t('login.passwordRequired') }]"
            type="password"
          />
        </van-cell-group>

        <div class="page__actions">
          <van-button
            block
            :loading="auth.loginLoading"
            native-type="submit"
            type="primary"
          >
            {{ t('login.submit') }}
          </van-button>
        </div>
      </van-form>
    </section>
  </main>
</template>

<style scoped src="../styles/page.css"></style>
