<script setup lang="ts">
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';

const router = useRouter();
const route = useRoute();
const { t } = useI18n();

const tabs = [
  { icon: 'wap-home-o', name: 'home', text: 'tabbar.home' },
  {
    icon: 'chart-trending-o',
    name: 'market',
    text: 'tabbar.market',
  },
  { icon: 'friends-o', name: 'team', text: 'tabbar.team' },
  { icon: 'balance-o', name: 'fund', text: 'tabbar.fund' },
  { icon: 'contact-o', name: 'my', text: 'tabbar.my' },
];
</script>

<template>
  <van-tabbar
    class="mobile-tabbar"
    :model-value="String(route.name)"
    safe-area-inset-bottom
  >
    <van-tabbar-item
      v-for="tab in tabs"
      :key="tab.name"
      :icon="tab.icon"
      :name="tab.name"
      @click="router.push({ name: tab.name })"
    >
      {{ t(tab.text) }}
    </van-tabbar-item>
  </van-tabbar>
</template>

<style scoped lang="scss">
.mobile-tabbar {
  --van-tabbar-height: var(--mobile-tabbar-height);
  --van-tabbar-item-font-size: var(--mobile-tabbar-font-size);
  --van-tabbar-item-icon-size: var(--mobile-tabbar-icon-size);
}
</style>
