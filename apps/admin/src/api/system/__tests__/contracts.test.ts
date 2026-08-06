import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  createAdmin,
  createI18nMessage,
  createMenu,
  createPermission,
  createRole,
  finalizeImageUpload,
  getAdminPage,
  getI18nMessageCategories,
  getI18nMessagePage,
  getI18nMessageValues,
  getImagePage,
  getMenuTree,
  getPermissionList,
  getRoleList,
  getRoleMenuIds,
  getRolePermissionIds,
  presignImageUpload,
  removeAdmin,
  removeI18nMessage,
  removeImage,
  removeMenu,
  removePermission,
  removeRole,
  saveAdminRoles,
  saveI18nMessage,
  saveRoleMenus,
  saveRolePermissions,
  updateAdmin,
  updateMenu,
  updatePermission,
  updateRole,
} from '..';
import { getI18nMessageBundle } from '../../core/i18n-message';

const request = vi.hoisted(() => ({ post: vi.fn() }));

vi.mock('#/api/request', () => ({ requestClient: request }));

describe('system management API contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    request.post.mockResolvedValue(undefined);
  });

  it('uses the backend administrator POST contracts', async () => {
    await getAdminPage({ currentPage: 2, pageSize: 20, username: 'alice' });
    await createAdmin({
      homePath: '/dashboard',
      nickname: 'Alice',
      password: 'Strong#123',
      status: true,
      username: 'alice',
    });
    await updateAdmin({ id: '7', nickname: 'Updated', password: '   ' });
    await saveAdminRoles('7', ['admin', 'rbac:manager']);
    await removeAdmin('7/unsafe');

    expect(request.post).toHaveBeenNthCalledWith(1, '/sys/admin/page', {
      currentPage: 2,
      pageSize: 20,
      username: 'alice',
    });
    expect(request.post).toHaveBeenNthCalledWith(
      2,
      '/sys/admin/create',
      expect.objectContaining({ password: 'Strong#123', username: 'alice' }),
    );
    expect(request.post).toHaveBeenNthCalledWith(3, '/sys/admin/update', {
      id: '7',
      nickname: 'Updated',
    });
    expect(request.post).toHaveBeenNthCalledWith(4, '/sys/admin/roles/save', {
      id: '7',
      roleCodes: ['admin', 'rbac:manager'],
    });
    expect(request.post).toHaveBeenNthCalledWith(
      5,
      '/sys/admin/remove/7%2Funsafe',
    );
  });

  it('uses separate role definition, permission, and menu grant contracts', async () => {
    await getRoleList({ name: 'Operator' });
    await createRole({ code: 'operator', name: 'Operator', remark: '' });
    await updateRole({ id: '3', code: 'operator', name: 'Ops', remark: '' });
    await getRolePermissionIds('3');
    await saveRolePermissions('3', ['8', '9']);
    await getRoleMenuIds('3');
    await saveRoleMenus('3', ['1', '2']);
    await removeRole('3');

    expect(request.post.mock.calls).toEqual([
      ['/authz/role/list', { name: 'Operator' }],
      [
        '/authz/role/create',
        { code: 'operator', name: 'Operator', remark: '' },
      ],
      [
        '/authz/role/update',
        { id: '3', code: 'operator', name: 'Ops', remark: '' },
      ],
      ['/authz/role-permission/list/3'],
      [
        '/authz/role-permission/save',
        { permissionIds: ['8', '9'], roleId: '3' },
      ],
      ['/authz/role-menu/list/3'],
      ['/authz/role-menu/save', { menuIds: ['1', '2'], roleId: '3' }],
      ['/authz/role/remove/3'],
    ]);
  });

  it('uses non-paged permission CRUD POST contracts', async () => {
    const data = {
      code: 'POST:/reports',
      name: 'Create report',
      publicAccess: false,
      remark: '',
      targetIdentifier: '/reports',
      targetQualifier: 'POST',
    };
    await getPermissionList({ publicAccess: false });
    await createPermission(data);
    await updatePermission({ id: '11', ...data });
    await removePermission('11');

    expect(request.post.mock.calls).toEqual([
      ['/authz/permission/list', { publicAccess: false }],
      ['/authz/permission/create', data],
      ['/authz/permission/update', { id: '11', ...data }],
      ['/authz/permission/remove/11'],
    ]);
  });

  it('uses tree menu CRUD POST contracts without runtime fields', async () => {
    const data = {
      affixTab: false,
      fullPathKey: true,
      hideChildrenInMenu: false,
      hideInBreadcrumb: false,
      hideInMenu: false,
      hideInTab: false,
      keepAlive: false,
      maxNumOfOpenTab: 0,
      name: 'Reports',
      noBasicLayout: false,
      openInNewWindow: false,
      order: 10,
      path: '/reports',
      pid: '0',
      status: true,
      title: 'menu.reports.title',
      type: 'catalog' as const,
    };
    await getMenuTree();
    await createMenu(data);
    await updateMenu({ id: '20', ...data });
    await removeMenu('20');

    expect(request.post.mock.calls).toEqual([
      ['/authz/menu/tree'],
      ['/authz/menu/create', data],
      ['/authz/menu/update', { id: '20', ...data }],
      ['/authz/menu/remove/20'],
    ]);
  });

  it('uses global message keys with category-scoped administration', async () => {
    const data = {
      category: 'default',
      messageKey: 'menu.example.title',
      values: [{ locale: 'en-US', value: 'Example' }],
    };

    await getI18nMessageCategories();
    await getI18nMessagePage({ category: 'default', key: 'menu.example' });
    await getI18nMessageValues('menu.example.title');
    await createI18nMessage(data);
    await saveI18nMessage(data);
    await removeI18nMessage('menu.example.title');
    await getI18nMessageBundle();

    expect(request.post.mock.calls).toEqual([
      ['/sys/i18n-message/categories'],
      ['/sys/i18n-message/page', { category: 'default', key: 'menu.example' }],
      ['/sys/i18n-message/values/menu.example.title'],
      ['/sys/i18n-message/create', data],
      ['/sys/i18n-message/save', data],
      ['/sys/i18n-message/remove/menu.example.title'],
      ['/sys/i18n-message/bundle/admin'],
    ]);
  });

  it('uses the direct image upload and management contracts', async () => {
    const image = {
      contentLength: 1024,
      contentType: 'image/png',
    };
    await presignImageUpload(image);
    await finalizeImageUpload('images/2026/08/05/file.png');
    await getImagePage({ currentPage: 2, pageSize: 10 });
    await removeImage('42/unsafe');

    expect(request.post.mock.calls).toEqual([
      ['/image/presign', image],
      ['/image/finalize', { objectKey: 'images/2026/08/05/file.png' }],
      ['/image/page', { currentPage: 2, pageSize: 10 }],
      ['/image/remove/42%2Funsafe'],
    ]);
  });
});
