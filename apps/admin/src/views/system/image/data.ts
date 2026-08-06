import type { VxeTableGridColumns } from '#/adapter/vxe-table';
import type { ImageApi } from '#/api';

import { $t } from '#/locales';

export function useColumns(): VxeTableGridColumns<ImageApi.Image> {
  return [
    {
      align: 'center',
      field: 'objectKey',
      slots: { default: 'preview' },
      title: $t('page.systemImage.table.preview'),
      width: 100,
    },
    {
      field: 'url',
      minWidth: 320,
      slots: { default: 'url' },
      title: $t('page.systemImage.table.url'),
    },
    {
      field: 'createTime',
      formatter: 'formatDateTime',
      title: $t('page.systemImage.table.createTime'),
      width: 180,
    },
    {
      align: 'center',
      field: 'operation',
      fixed: 'right',
      slots: { default: 'action' },
      title: $t('page.rbacCommon.actions'),
      width: 120,
    },
  ];
}
