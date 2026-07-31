import {
  appCopyrightPreferences,
  defineOverridesPreferences,
} from '@vben/preferences';

/**
 * @description 项目配置文件
 * 只需要覆盖项目中的一部分配置，不需要的配置不用覆盖，会自动使用默认配置
 * !!! 更改配置后请清空缓存，否则可能不生效
 */
export const overridesPreferences = defineOverridesPreferences({
  // overrides
  app: {
    accessMode: 'backend',
    defaultAvatar: '',
    defaultHomePath: '/dashboard',
    enableRefreshToken: true,
    name: import.meta.env.VITE_APP_TITLE,
  },
  copyright: {
    ...appCopyrightPreferences,
    companyName: 'Novum',
    companySiteLink: 'https://github.com/1gnilc/novum',
    date: '2026',
    enable: false,
    settingShow: false,
  },
  shortcutKeys: {
    globalLockScreen: false,
  },
  widget: {
    lockScreen: false,
    logoutButtonPosition: 'user-dropdown',
  },
  theme: {
    radius: '0.25',
    fontSize: 15,
  },
});
