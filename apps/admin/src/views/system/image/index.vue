<script setup lang="ts">
import type { VxeTableGridOptions } from '#/adapter/vxe-table';
import type { ImageApi } from '#/api';

import { Page, useVbenDrawer, VbenButton } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import { useClipboard } from '@vueuse/core';
import { ElButton, ElImage, ElMessage, ElTooltip } from 'element-plus';

import { useVbenVxeGrid, VbenTableAction } from '#/adapter/vxe-table';
import { getImagePage, removeImage } from '#/api';
import { $t } from '#/locales';

import { useColumns } from './data';
import Form from './modules/form.vue';

const { copy } = useClipboard({ legacy: true });

const [FormDrawer, formDrawerApi] = useVbenDrawer({
  connectedComponent: Form,
  destroyOnClose: true,
});

const [Grid, gridApi] = useVbenVxeGrid<ImageApi.Image>({
  gridOptions: {
    columns: useColumns(),
    height: 'auto',
    keepSource: true,
    pagerConfig: {},
    proxyConfig: {
      ajax: {
        async query({ page }) {
          const result = await getImagePage({
            currentPage: page.currentPage,
            pageSize: page.pageSize,
          });
          return { list: result.list, total: result.totalCount };
        },
      },
      showLoading: false,
    },
    rowConfig: { height: 80, keyField: 'id' },
    toolbarConfig: { custom: true, refresh: true, zoom: true },
  } as VxeTableGridOptions<ImageApi.Image>,
});

function onPreviewKeydown(event: KeyboardEvent) {
  (event.currentTarget as HTMLElement).click();
}

function onUpload() {
  formDrawerApi.open();
}

async function onCopyUrl(url: string) {
  await copy(url);
  ElMessage.success($t('page.systemImage.messages.copySuccess'));
}

async function onDelete(row: ImageApi.Image) {
  await removeImage(row.id);
  ElMessage.success($t('page.systemImage.messages.removeSuccess'));
  await gridApi.query();
}
</script>

<template>
  <Page auto-content-height>
    <FormDrawer @success="gridApi.query()" />
    <Grid :table-title="$t('page.systemImage.title')">
      <template #toolbar-tools>
        <VbenButton size="sm" @click="onUpload">
          <IconifyIcon icon="lucide:upload" class="mr-2 size-4" />
          {{ $t('page.systemImage.actions.upload') }}
        </VbenButton>
      </template>

      <template #preview="{ row }">
        <ElImage
          :alt="$t('page.systemImage.actions.previewImage')"
          :aria-label="$t('page.systemImage.actions.previewImage')"
          :preview-src-list="[row.url]"
          :src="row.url"
          class="image-management__preview"
          fit="cover"
          preview-teleported
          role="button"
          tabindex="0"
          @keydown.enter.prevent="onPreviewKeydown"
          @keydown.space.prevent="onPreviewKeydown"
        />
      </template>

      <template #url="{ row }">
        <div class="image-management__url">
          <span class="image-management__url-text">{{ row.url }}</span>
          <ElTooltip
            :content="$t('page.systemImage.actions.copyUrl')"
            placement="top"
          >
            <ElButton
              :aria-label="$t('page.systemImage.actions.copyUrl')"
              circle
              text
              @click.stop="onCopyUrl(row.url)"
            >
              <IconifyIcon class="size-4" icon="lucide:copy" />
            </ElButton>
          </ElTooltip>
        </div>
      </template>

      <template #action="{ row }">
        <VbenTableAction
          :dropdown-actions="[
            {
              auth: 'system:image:remove',
              danger: true,
              text: $t('page.rbacCommon.remove'),
              popConfirm: {
                title: $t('page.systemImage.messages.removeConfirm'),
                confirm: () => onDelete(row),
              },
            },
          ]"
          align="center"
        />
      </template>
    </Grid>
  </Page>
</template>

<style scoped>
.image-management__preview {
  width: 56px;
  height: 56px;
  border-radius: 4px;
}

.image-management__preview :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
}

.image-management__url {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.image-management__url-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
