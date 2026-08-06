<script setup lang="ts">
import type { UploadFile, UploadFiles, UploadUserFile } from 'element-plus';

import type { VbenFormSchema } from '#/adapter/form';

import { h, nextTick, ref } from 'vue';

import { useVbenDrawer } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { ElMessage } from 'element-plus';

import { useVbenForm } from '#/adapter/form';
import {
  IMAGE_TYPES,
  uploadImage,
  validateImage,
} from '#/composables/use-image-upload';
import { $t } from '#/locales';

import { confirmDrawerClose } from '../../components/dirty';

const emit = defineEmits<{ success: [] }>();

interface ImageUploadForm {
  files: UploadUserFile[];
}

const saved = ref(false);
const selected = ref(false);

function onFileChange(file: UploadFile, files: UploadFiles) {
  if (!file.raw) return;
  try {
    validateImage(file.raw);
    selected.value = true;
  } catch {
    if (file.url?.startsWith('blob:')) URL.revokeObjectURL(file.url);
    files.splice(0);
    selected.value = false;
    void formApi.setFieldValue('files', [], false);
    ElMessage.error($t('page.systemImage.validation.invalidFile'));
  }
}

function onFileRemove() {
  selected.value = false;
}

async function handleSubmit(values: ImageUploadForm) {
  const file = values.files[0]?.raw;
  if (!file) {
    ElMessage.error($t('page.systemImage.validation.fileRequired'));
    return;
  }

  drawerApi.lock();
  try {
    await uploadImage(file);
    saved.value = true;
    ElMessage.success($t('page.systemImage.messages.uploadSuccess'));
    emit('success');
    await drawerApi.close();
  } catch {
    ElMessage.error($t('page.systemImage.messages.uploadFailed'));
  } finally {
    drawerApi.unlock();
  }
}

const schema: VbenFormSchema<ImageUploadForm>[] = [
  {
    component: 'Upload',
    componentProps: {
      accept: IMAGE_TYPES.join(','),
      autoUpload: false,
      limit: 1,
      listType: 'picture-card',
      onChange: onFileChange,
      onExceed: () =>
        ElMessage.warning($t('page.systemImage.validation.singleFile')),
      onRemove: onFileRemove,
    },
    defaultValue: [],
    dependencies: {
      resolve: ({ values }) => ({
        componentProps: {
          class: values.files?.length
            ? 'image-upload-form__field--selected'
            : '',
        },
      }),
      triggerFields: ['files'],
    },
    description: $t('page.systemImage.form.fileHint'),
    fieldName: 'files',
    formItemClass: 'col-span-full',
    label: $t('page.systemImage.form.file'),
    renderComponentContent: () => ({
      default: () =>
        h(IconifyIcon, {
          'aria-label': $t('page.systemImage.actions.selectFile'),
          class: 'size-6',
          icon: 'lucide:plus',
        }),
    }),
    rules: 'required',
  },
];

const [Form, formApi] = useVbenForm<ImageUploadForm>({
  commonConfig: { componentProps: { class: 'w-full' } },
  handleSubmit,
  schema,
  showDefaultActions: false,
  wrapperClass: 'grid-cols-1',
});

const [Drawer, drawerApi] = useVbenDrawer({
  async onBeforeClose() {
    if (saved.value) return true;
    return confirmDrawerClose(selected.value);
  },
  async onConfirm() {
    await formApi.validateAndSubmit();
  },
  async onOpenChange(open) {
    if (!open) return;
    saved.value = false;
    selected.value = false;
    drawerApi.setState({ title: $t('page.systemImage.drawer.uploadTitle') });
    await formApi.reset();
    await nextTick();
    await formApi.setValues({ files: [] }, false);
  },
});
</script>

<template>
  <Drawer class="w-full sm:max-w-lg">
    <Form />
  </Drawer>
</template>

<style scoped>
:deep(.image-upload-form__field--selected .el-upload--picture-card) {
  display: none;
}
</style>
