<script setup lang="ts">
import type { VbenFormSchema } from '#/adapter/form';
import type { I18nMessageApi } from '#/api';

import { nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { isEqual, trimToNull } from '@vben/utils';

import { ElMessage } from 'element-plus';

import { useVbenForm, z } from '#/adapter/form';
import { createI18nMessage, saveI18nMessage } from '#/api';
import { $t } from '#/locales';
import { reloadDynamicMessages } from '#/locales/dynamic';

import { confirmDrawerClose } from '../../components/dirty';
import {
  I18N_MESSAGE_INPUT_MAX_LENGTH,
  I18N_MESSAGE_MAX_CODE_POINTS,
} from '../validation';

export interface I18nMessageFormDrawerData {
  categories: string[];
  row?: I18nMessageApi.MessageItem;
}

interface MessageForm {
  category: string;
  editing: boolean;
  enUS: string;
  messageKey: string;
  zhCN?: string;
}

const emit = defineEmits<{ success: [] }>();

const categories = ref<string[]>([]);
const initialValues = ref<MessageForm>();
const saved = ref(false);

const messageKeyRule = z
  .string()
  .trim()
  .min(1, { message: $t('page.i18nMessage.validation.keyRequired') })
  .max(191, { message: $t('page.i18nMessage.validation.keyTooLong') })
  .regex(/^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)*$/, {
    message: $t('page.i18nMessage.validation.keyInvalid'),
  });

const schema: VbenFormSchema<MessageForm>[] = [
  { component: 'Input', fieldName: 'editing', hide: true },
  {
    component: 'Select',
    componentProps: () => ({
      options: categories.value.map((value) => ({ label: value, value })),
    }),
    fieldName: 'category',
    label: $t('page.i18nMessage.form.category'),
    rules: 'selectRequired',
  },
  {
    component: 'Input',
    dependencies: {
      resolve: ({ values }) => ({
        componentProps: { disabled: Boolean(values.editing) },
      }),
      triggerFields: ['editing'],
    },
    fieldName: 'messageKey',
    label: $t('page.i18nMessage.form.messageKey'),
    rules: messageKeyRule,
  },
  {
    component: 'Input',
    componentProps: {
      maxlength: I18N_MESSAGE_INPUT_MAX_LENGTH,
      rows: 4,
      type: 'textarea',
    },
    fieldName: 'enUS',
    label: 'en-US',
    rules: z
      .string()
      .trim()
      .min(1, { message: $t('page.i18nMessage.validation.enRequired') })
      .refine((value) => [...value].length <= I18N_MESSAGE_MAX_CODE_POINTS, {
        message: $t('page.i18nMessage.validation.valueTooLong'),
      }),
  },
  {
    component: 'Input',
    componentProps: {
      maxlength: I18N_MESSAGE_INPUT_MAX_LENGTH,
      rows: 4,
      type: 'textarea',
    },
    fieldName: 'zhCN',
    label: 'zh-CN',
    rules: z
      .string()
      .trim()
      .refine((value) => [...value].length <= I18N_MESSAGE_MAX_CODE_POINTS, {
        message: $t('page.i18nMessage.validation.valueTooLong'),
      })
      .optional(),
  },
];

const [Form, formApi] = useVbenForm({
  commonConfig: { componentProps: { class: 'w-full' } },
  schema,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1',
});

function valueFor(row: I18nMessageApi.MessageItem, locale: string) {
  return row.values.find((item) => item.locale === locale)?.value ?? '';
}

const [Drawer, drawerApi] = useVbenDrawer({
  async onBeforeClose() {
    if (saved.value) return true;
    return confirmDrawerClose(
      !isEqual(await formApi.getValues(), initialValues.value),
    );
  },
  async onConfirm() {
    const { valid } = await formApi.validate();
    if (!valid) return;
    const values = await formApi.getValues<MessageForm>();
    trimToNull(values);
    drawerApi.lock();
    try {
      const persistMessage = values.editing
        ? saveI18nMessage
        : createI18nMessage;
      try {
        await persistMessage({
          category: values.category,
          messageKey: values.messageKey,
          values: [
            { locale: 'en-US', value: values.enUS },
            { locale: 'zh-CN', value: values.zhCN ?? '' },
          ],
        });
      } catch {
        return;
      }
      saved.value = true;
      ElMessage.success($t('page.i18nMessage.messages.saveSuccess'));
      if (
        values.category === 'admin' ||
        initialValues.value?.category === 'admin'
      ) {
        try {
          await reloadDynamicMessages();
        } catch {
          ElMessage.warning(
            $t('page.i18nMessage.messages.runtimeReloadFailed'),
          );
        }
      }
      emit('success');
      await drawerApi.close();
    } finally {
      drawerApi.unlock();
    }
  },
  async onOpenChange(open) {
    if (!open) return;
    saved.value = false;
    const payload = drawerApi.getData<I18nMessageFormDrawerData>();
    categories.value = payload.categories;
    const row = payload.row;
    const values: MessageForm = {
      category: row?.category ?? categories.value[0] ?? '',
      editing: !!row,
      enUS: row ? valueFor(row, 'en-US') : '',
      messageKey: row?.messageKey ?? '',
      zhCN: row ? valueFor(row, 'zh-CN') : '',
    };
    drawerApi.setState({
      title: row
        ? $t('page.i18nMessage.drawer.editTitle')
        : $t('page.i18nMessage.drawer.createTitle'),
    });
    await formApi.reset();
    await nextTick();
    await formApi.setValues(values, false);
    initialValues.value = await formApi.getValues<MessageForm>();
  },
});
</script>

<template>
  <Drawer class="w-full sm:max-w-2xl">
    <Form />
  </Drawer>
</template>
