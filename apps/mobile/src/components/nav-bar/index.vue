<script setup lang="ts">
import { computed, useAttrs, useSlots } from 'vue';
import { useRouter } from 'vue-router';

import { navBarProps } from 'vant';

import Locale from '#/components/locale/index.vue';

defineOptions({ inheritAttrs: false });

const props = defineProps({
  ...navBarProps,
  fixed: {
    default: true,
    type: Boolean,
  },
  locale: {
    default: false,
    type: Boolean,
  },
  placeholder: {
    default: true,
    type: Boolean,
  },
  safeAreaInsetTop: {
    default: true,
    type: Boolean,
  },
});

const emit = defineEmits<{
  clickLeft: [event: MouseEvent];
  clickRight: [event: MouseEvent];
}>();

const attrs = useAttrs();
const router = useRouter();
const slots = useSlots();

const navBarBindings = computed(() => {
  const { locale, ...bindings } = props;

  return {
    ...bindings,
    ...attrs,
    leftArrow: !locale && props.leftArrow,
  };
});

function onClickLeft(event: MouseEvent) {
  if (props.locale) return;

  emit('clickLeft', event);
  if (!slots.left && props.leftArrow) router.back();
}

function onClickRight(event: MouseEvent) {
  emit('clickRight', event);
}
</script>

<template>
  <van-nav-bar
    v-bind="navBarBindings"
    @click-left="onClickLeft"
    @click-right="onClickRight"
  >
    <template v-if="locale || $slots.left" #left>
      <Locale v-if="locale" />
      <slot v-else name="left"></slot>
    </template>
    <template v-if="$slots.title" #title>
      <slot name="title"></slot>
    </template>
    <template v-if="$slots.right" #right>
      <slot name="right"></slot>
    </template>
  </van-nav-bar>
</template>
