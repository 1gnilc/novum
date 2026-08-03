import type { ConfigProviderThemeVars } from 'vant';

import { reactive } from 'vue';

// themeVars 内的值会被转换成对应 CSS 变量
// 比如 sliderBarHeight 会转换成 `--van-slider-bar-height`
const themeVars = reactive<ConfigProviderThemeVars>({
  // rateIconFullColor: '#07c160',
  // sliderBarHeight: '4px',
});

export { themeVars };
