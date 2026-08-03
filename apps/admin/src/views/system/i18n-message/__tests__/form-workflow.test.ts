/* eslint-disable vue/one-component-per-file -- Local stubs keep the workflow test focused on drawer behavior. */
import { shallowMount } from '@vue/test-utils';

import { beforeEach, describe, expect, it, vi } from 'vitest';

import I18nMessageForm from '../modules/form.vue';

const runtime = vi.hoisted(() => ({
  api: {
    createI18nMessage: vi.fn(),
    saveI18nMessage: vi.fn(),
  },
  drawerApi: {
    close: vi.fn(),
    getData: vi.fn(),
    lock: vi.fn(),
    setState: vi.fn(),
    unlock: vi.fn(),
  },
  drawerData: {} as Record<string, unknown>,
  drawerOptions: undefined as Record<string, any> | undefined,
  formApi: {
    getValues: vi.fn(),
    reset: vi.fn(),
    setValues: vi.fn(),
    validate: vi.fn(),
  },
  formValues: {} as Record<string, unknown>,
  messages: {
    success: vi.fn(),
    warning: vi.fn(),
  },
  reloadDynamicMessages: vi.fn(),
  valueLimitChecks: [] as Array<(value: string) => boolean>,
}));

vi.mock('@vben/common-ui', async () => {
  const { defineComponent } = await import('vue');
  const Empty = defineComponent({ render: () => null });
  return {
    useVbenDrawer: (options: Record<string, any>) => {
      runtime.drawerOptions = options;
      return [Empty, runtime.drawerApi];
    },
  };
});

vi.mock('#/adapter/form', async () => {
  const { defineComponent } = await import('vue');
  const Empty = defineComponent({ render: () => null });
  const stringRule: Record<string, any> = {};
  for (const method of ['max', 'min', 'optional', 'regex', 'trim']) {
    stringRule[method] = vi.fn(() => stringRule);
  }
  stringRule.refine = vi.fn((check: (value: string) => boolean) => {
    runtime.valueLimitChecks.push(check);
    return stringRule;
  });
  return {
    useVbenForm: () => [Empty, runtime.formApi],
    z: { string: () => stringRule },
  };
});

vi.mock('#/api', () => runtime.api);
vi.mock('#/locales', () => ({ $t: (key: string) => key }));
vi.mock('#/locales/dynamic', () => ({
  reloadDynamicMessages: runtime.reloadDynamicMessages,
}));
vi.mock('../../components/dirty', () => ({
  confirmDrawerClose: vi.fn(),
}));
vi.mock('element-plus', () => ({ ElMessage: runtime.messages }));

describe('internationalization message form workflow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    runtime.valueLimitChecks = [];
    runtime.drawerData = {};
    runtime.drawerApi.getData.mockImplementation(() => runtime.drawerData);
    runtime.drawerApi.close.mockResolvedValue(undefined);
    runtime.formApi.getValues.mockImplementation(async () => ({
      ...runtime.formValues,
    }));
    runtime.formApi.reset.mockResolvedValue(undefined);
    runtime.formApi.setValues.mockImplementation(async (values) => {
      runtime.formValues = { ...values };
    });
    runtime.formApi.validate.mockResolvedValue({ valid: true });
    runtime.api.createI18nMessage.mockResolvedValue(undefined);
    runtime.api.saveI18nMessage.mockResolvedValue(undefined);
    runtime.reloadDynamicMessages.mockResolvedValue(undefined);
  });

  it('validates locale value limits by Unicode code points', () => {
    const supplementaryCharacter = '\u{1F600}';
    const wrapper = shallowMount(I18nMessageForm);

    expect(runtime.valueLimitChecks).toHaveLength(2);
    for (const check of runtime.valueLimitChecks) {
      expect(check(supplementaryCharacter.repeat(4000))).toBe(true);
      expect(check(supplementaryCharacter.repeat(4001))).toBe(false);
    }
    wrapper.unmount();
  });

  it('creates a new message through the create-only endpoint', async () => {
    runtime.drawerData = { categories: ['default', 'admin'] };
    const wrapper = shallowMount(I18nMessageForm);
    const options = runtime.drawerOptions;
    if (!options) throw new Error('Drawer options were not captured');
    await options.onOpenChange(true);
    runtime.formValues = {
      category: 'default',
      editing: false,
      enUS: 'New message',
      messageKey: 'test.create.title',
      zhCN: '新增消息',
    };

    await options.onConfirm();

    expect(runtime.api.createI18nMessage).toHaveBeenCalledWith({
      category: 'default',
      messageKey: 'test.create.title',
      values: [
        { locale: 'en-US', value: 'New message' },
        { locale: 'zh-CN', value: '新增消息' },
      ],
    });
    expect(runtime.api.saveI18nMessage).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it('continues to edit an existing message through the save endpoint', async () => {
    runtime.drawerData = {
      categories: ['default', 'admin'],
      row: {
        category: 'admin',
        messageKey: 'test.edit.title',
        values: [{ locale: 'en-US', value: 'Existing message' }],
      },
    };
    const wrapper = shallowMount(I18nMessageForm);
    const options = runtime.drawerOptions;
    if (!options) throw new Error('Drawer options were not captured');
    await options.onOpenChange(true);
    runtime.formValues = {
      ...runtime.formValues,
      enUS: 'Updated message',
    };

    await options.onConfirm();

    expect(runtime.api.saveI18nMessage).toHaveBeenCalledWith(
      expect.objectContaining({
        category: 'admin',
        messageKey: 'test.edit.title',
        values: expect.arrayContaining([
          { locale: 'en-US', value: 'Updated message' },
        ]),
      }),
    );
    expect(runtime.api.createI18nMessage).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it('keeps failed input editable and allows save retry without reopening', async () => {
    runtime.drawerData = { categories: ['default', 'admin'] };
    const wrapper = shallowMount(I18nMessageForm);
    const options = runtime.drawerOptions;
    if (!options) throw new Error('Drawer options were not captured');
    await options.onOpenChange(true);
    runtime.formValues = {
      category: 'admin',
      editing: false,
      enUS: 'Retry message',
      messageKey: 'test.retry.title',
      zhCN: '重试消息',
    };

    runtime.api.createI18nMessage.mockRejectedValueOnce(new Error('timeout'));
    await expect(options.onConfirm()).resolves.toBeUndefined();

    expect(runtime.drawerApi.lock).toHaveBeenCalledOnce();
    expect(runtime.drawerApi.unlock).toHaveBeenCalledOnce();
    expect(runtime.drawerApi.close).not.toHaveBeenCalled();
    expect(wrapper.emitted('success')).toBeUndefined();

    runtime.api.createI18nMessage.mockResolvedValue(undefined);
    await expect(options.onConfirm()).resolves.toBeUndefined();

    expect(runtime.api.createI18nMessage).toHaveBeenCalledTimes(2);
    expect(runtime.api.createI18nMessage).toHaveBeenLastCalledWith({
      category: 'admin',
      messageKey: 'test.retry.title',
      values: [
        { locale: 'en-US', value: 'Retry message' },
        { locale: 'zh-CN', value: '重试消息' },
      ],
    });
    expect(runtime.drawerApi.unlock).toHaveBeenCalledTimes(2);
    expect(runtime.drawerApi.close).toHaveBeenCalledOnce();
    expect(wrapper.emitted('success')).toHaveLength(1);

    wrapper.unmount();
  });
});
