# Mobile Vant 一手资料研究

> - 检索日期：2026-08-01
> - 范围：Vant 4 在 Vue 3 + Vite 项目中的接入、按需引入组件与样式、使用提示、底部安全区，以及官方 `vant-demo/vant/vite` 的参考价值。
> - 资料原则：外部结论只引用 Vant、相关插件维护者、npm 官方 registry 和官方 demo 仓库等一手来源。

## 结论摘要

1. `apps/mobile` 应使用 Vant 4。检索时 npm `latest` 和 Vant 官方仓库中的包版本均为 `4.10.0`；Vant 4 的 `vue` peer dependency 是 `^3.0.0`，因此仓库 catalog 中的 Vue 3 符合其运行时前提。[Vant 官方包清单（固定提交）](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant/package.json) · [npm 官方 registry](https://registry.npmjs.org/vant/latest)
2. 用户要求的“按需引入组件样式”应采用 Vant 官方提供的 `unplugin-vue-components` + `unplugin-auto-import` + `@vant/auto-import-resolver` 方案。`Components` 负责模板组件，`AutoImport` 负责 `showToast` 等函数 API；两处都配置 `VantResolver()`，解析器默认按组件/API自动引入对应样式。官方说明常规方案更简单，但本项目已有明确的按需 CSS 要求，因此选择方法二。[快速上手](https://vant-ui.github.io/vant/#/zh-CN/quickstart) · [解析器 README（固定提交）](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/README.zh-CN.md) · [解析器源码（固定提交）](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/src/index.ts)
3. 不得同时保留 `app.use(Vant)`、`import 'vant/lib/index.css'` 等全量引入与上述按需方案。Vant 官方明确提示混用会造成重复代码、样式错乱；Tree Shaking 只能自动移除未使用的 JS，不能让全量 CSS 变成按需 CSS。[快速上手“引入组件/使用提示”](https://vant-ui.github.io/vant/#/zh-CN/quickstart)
4. 底部安全区不只是一项组件 prop：页面 `viewport` 必须包含 `viewport-fit=cover`。之后可使用组件的 `safe-area-inset-bottom`，或对自有元素使用 `van-safe-area-bottom`；Vant 的工具类最终使用 `constant()`/`env(safe-area-inset-bottom)` 增加底部 padding。[进阶用法“底部安全区适配”](https://vant-ui.github.io/vant/#/zh-CN/advanced-usage) · [内置样式](https://vant-ui.github.io/vant/#/zh-CN/style) · [样式源码（固定提交）](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant/src/style/base.less)
5. 登录询问的首选 Vant 容器是 `ActionSheet`：它的官方语义就是“底部弹起的模态面板”，原生提供操作项、取消按钮、选择/取消事件，并且 `safe-area-inset-bottom` 默认开启。只有需要完全自定义复杂内容时再考虑 `Popup position="bottom"`；Popup 的安全区默认关闭，必须显式开启。[ActionSheet](https://vant-ui.github.io/vant/#/zh-CN/action-sheet) · [Popup](https://vant-ui.github.io/vant/#/zh-CN/popup)
6. 已克隆的 Vite demo 只适合参考插件接线形状，不适合复制为项目基线。该子树最后一次提交是 2024-03-17，依赖和代码形态都明显早于当前仓库工具链；它还包含未安装、未挂载 `vue-router` 却调用 `this.$router` 的不可运行路径。[demo 历史（固定提交）](https://github.com/vant-ui/vant-demo/commit/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b) · [package.json](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/package.json) · [main.js](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/src/main.js) · [App.vue](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/src/App.vue)

## Vue 3 + Vite 接入方案

### 依赖角色

以下版本是检索日从 npm 官方 registry 核验到的最新版，不代表应在应用包内写游离版本；在本仓库中应先加入根 catalog，再由 `apps/mobile/package.json` 使用 `catalog:`。

| 包 | 检索日版本 | 依赖类型 | 作用 | 结论 | 一手来源 |
| --- | --: | --- | --- | --- | --- |
| `vant` | `4.10.0` | `dependencies` | Vue 3 移动端组件库 | 新增；应用运行时依赖 | [npm registry](https://registry.npmjs.org/vant/latest) · [官方包清单](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant/package.json) |
| `@vant/auto-import-resolver` | `1.3.0` | `devDependencies` | 将 Vant 组件/API映射到 ESM 模块及对应样式 | 新增；`importStyle` 默认 `true`，`module` 默认 `esm` | [npm registry](https://registry.npmjs.org/@vant%2fauto-import-resolver/latest) · [官方 README](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/README.zh-CN.md) |
| `unplugin-vue-components` | `32.1.0` | `devDependencies` | 扫描模板并自动引入/注册 `Van*` 组件 | 新增；与 `VantResolver()` 配合 | [npm registry](https://registry.npmjs.org/unplugin-vue-components/latest) · [维护者仓库](https://github.com/unplugin/unplugin-vue-components) |
| `unplugin-auto-import` | `21.0.0` | `devDependencies` | 自动引入 `showToast`、`showDialog` 等 Vant 函数 API | 新增；与 `VantResolver()` 配合 | [npm registry](https://registry.npmjs.org/unplugin-auto-import/latest) · [维护者仓库](https://github.com/unplugin/unplugin-auto-import) |
| `less` | demo 为 `^4.1.3` | - | demo 自己的 `<style lang="less">` 编译 | 不因 Vant 而新增；Vant 按需样式由解析器引用已构建样式入口 | [demo package.json](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/package.json) · [解析器源码](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/src/index.ts) |
| `@vant/touch-emulator` | 可选 | - | 把桌面鼠标事件转换为触摸事件 | 面向真实移动端的首期基线不需要；只有明确要求桌面交互兼容时再加入 | [进阶用法“桌面端适配”](https://vant-ui.github.io/vant/#/zh-CN/advanced-usage) |

### Vite 配置形状

官方 Vite 方案的核心如下；实现时应把两个插件加入 Mobile 本地的原生 Vite 配置，并沿用仓库 catalog 版本，不直接复制这段示例或接入 Admin 的 Vite 配置工厂：

```ts
import { VantResolver } from '@vant/auto-import-resolver';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';

export default {
  plugins: [
    AutoImport({
      resolvers: [VantResolver()],
    }),
    Components({
      resolvers: [VantResolver()],
    }),
  ],
};
```

来源：[Vant 快速上手](https://vant-ui.github.io/vant/#/zh-CN/quickstart) · [解析器 Vite 用法](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/README.zh-CN.md)

解析器的具体行为值得在实现评审时检查：

- 模板中的 `<van-button>` 会解析为 `VanButton`，组件插件再通过 resolver 导入 `vant/es` 中的 `Button`，并把 `vant/es/button/style/index` 作为 side effect 引入。[解析器源码](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/src/index.ts)
- `showToast`、`showConfirmDialog`、`showNotify`、`showImagePreview` 等函数 API 有显式映射；API 的样式也由同一个 resolver 作为 side effect 引入。[解析器源码](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/src/index.ts)
- `VantResolver()` 的样式自动引入默认开启，模块默认 ESM；本项目不应改为 `importStyle: false` 或 CJS，除非另有经过验证的构建约束。[解析器 README](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/README.zh-CN.md)

### 官方使用提示转化为验收项

| 官方提示 | 对 `apps/mobile` 的验收要求 | 来源 |
| --- | --- | --- |
| 不混用全量与按需引入 | `main.ts` 不得 `app.use(Vant)`，也不得引入 `vant/lib/index.css`；Vant 组件/API统一走 resolver 或明确的局部 import | [快速上手“使用提示”](https://vant-ui.github.io/vant/#/zh-CN/quickstart) |
| Tree Shaking 不能优化全量 CSS | 不能以“Vite 会 tree-shake”为理由保留全量样式入口 | [快速上手“方法一/方法二”](https://vant-ui.github.io/vant/#/zh-CN/quickstart) |
| 自动导入函数 API 时不要再显式 import | `showToast`、`showDialog` 等已由 `AutoImport` + resolver 管理的 API 直接调用；显式 import 会绕过 resolver 的样式 side effect，除非同时手工引入对应样式 | [Toast 常见问题](https://vant-ui.github.io/vant/#/zh-CN/toast) |
| `unplugin-vue-components` 不是 Vant 维护 | 组件解析问题按官方提示向插件仓库定位；样式问题再向 Vant 定位 | [快速上手“使用提示”](https://vant-ui.github.io/vant/#/zh-CN/quickstart) |
| Vant 默认面向移动触摸事件 | 不把桌面鼠标行为当作移动端验收依据；若产品确需桌面交互，再评估 `@vant/touch-emulator` | [进阶用法“桌面端适配”](https://vant-ui.github.io/vant/#/zh-CN/advanced-usage) |
| px 转 vw/rem 时不能忽略 `node_modules` | 首期如果引入 PostCSS 单位转换，必须让 Vant 样式参与转换；不要照搬过滤 `node_modules` 的常见配置 | [进阶用法“Viewport/Rem 布局适配”](https://vant-ui.github.io/vant/#/zh-CN/advanced-usage) |

## 底部安全区与登录询问

### 页面级前提

`apps/mobile/index.html` 的 viewport 应包含官方要求的 `viewport-fit=cover`：

```html
<meta
  name="viewport"
  content="width=device-width, initial-scale=1.0, viewport-fit=cover"
/>
```

这里保留项目现有的缩放策略，只增加安全区所需的 `viewport-fit=cover`，不照搬官方示例中无关的最大/最小缩放限制。仅设置组件的 `safe-area-inset-bottom` 而遗漏这个 meta，不能算完整的安全区接入。[进阶用法“底部安全区适配”](https://vant-ui.github.io/vant/#/zh-CN/advanced-usage)

对于 Vant 未封装的自有固定底栏，可使用 `van-safe-area-bottom` 工具类；它通过 `constant(safe-area-inset-bottom)` 和 `env(safe-area-inset-bottom)` 增加底部 padding。[内置样式“安全区”](https://vant-ui.github.io/vant/#/zh-CN/style) · [实现源码](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant/src/style/base.less)

### ActionSheet 与 Popup 比较

| 能力 | `ActionSheet` | `Popup position="bottom"` | 设计结论 | 来源 |
| --- | --- | --- | --- | --- |
| 语义 | 当前情境相关操作的底部模态面板 | 通用弹出层容器 | “前往登录 / 取消”优先 ActionSheet | [ActionSheet](https://vant-ui.github.io/vant/#/zh-CN/action-sheet) · [Popup](https://vant-ui.github.io/vant/#/zh-CN/popup) |
| 操作建模 | `actions`、`cancel-text`、`select`、`cancel` 原生支持 | 操作区需自行实现 | ActionSheet 可减少自定义交互代码 | [ActionSheet API](https://vant-ui.github.io/vant/#/zh-CN/action-sheet) |
| 底部安全区默认值 | `true` | `false` | ActionSheet 自动覆盖常规底部指示条；Popup 必须显式写 `safe-area-inset-bottom` | [ActionSheet Props](https://vant-ui.github.io/vant/#/zh-CN/action-sheet) · [Popup Props](https://vant-ui.github.io/vant/#/zh-CN/popup) |
| 路由回退时关闭 | `close-on-popstate` 默认 `true` | 默认 `false` | 全局登录询问更适合 ActionSheet 的默认行为，但仍应由公共认证状态在路由切换时重置 | [ActionSheet Props](https://vant-ui.github.io/vant/#/zh-CN/action-sheet) · [Popup Props](https://vant-ui.github.io/vant/#/zh-CN/popup) |
| 挂载位置 | 支持 `teleport` | 支持 `teleport` | 全局宿主可 `teleport="body"`，避免受页面盒模型、overflow 和 stacking context 影响 | [ActionSheet Props](https://vant-ui.github.io/vant/#/zh-CN/action-sheet) · [Popup“指定挂载位置”](https://vant-ui.github.io/vant/#/zh-CN/popup) |

补充：固定底部 Tabbar 开启 `fixed` 时会默认启用底部安全区，官方组件源码的判断是 `safeAreaInsetBottom ?? fixed`；无需在每个固定 Tabbar 上重复手工 padding。[Tabbar 文档](https://vant-ui.github.io/vant/#/zh-CN/tabbar) · [Tabbar 源码（固定提交）](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant/src/tabbar/Tabbar.tsx)

## 官方 Vite Demo 审核

本地参考目录：`/Volumes/fc/novum/vant-demo/vant/vite`。其 Vite 子树最后一次提交为 [`14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b`](https://github.com/vant-ui/vant-demo/commit/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b)，提交时间 2024-03-17。

### 版本差异

| 包 | demo 声明 | 2026-08-01 官方 registry / 当前仓库 | 处理 |
| --- | --: | --: | --- |
| `vant` | `^4.8.5` | `4.10.0` | 不复制 demo 版本；使用根 catalog 管理当前版本 |
| `vue` | `^3.4.21` | 仓库 catalog `^3.5.38` | 保留仓库 catalog，不降级 |
| `@vant/auto-import-resolver` | `^1.1.0` | `1.3.0` | 新增当前版本到 catalog |
| `unplugin-auto-import` | `^0.17.5` | `21.0.0` | 不复制历史大版本；以当前工具链安装/构建验证为准 |
| `unplugin-vue-components` | `^0.26.0` | `32.1.0` | 不复制历史大版本；以当前工具链安装/构建验证为准 |
| `@vitejs/plugin-vue` | `^4.3.4` | 仓库 catalog `^6.0.7` | 复用仓库 catalog，不在 mobile 单独降级 |
| `vite` | `^5.1.6` | 仓库 catalog `8.0.10` | 复用仓库工具链，不在 mobile 单独降级 |

demo 声明来源：[固定提交 package.json](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/package.json)。当前 Vant 与 resolver 版本来源：[Vant registry](https://registry.npmjs.org/vant/latest) · [resolver registry](https://registry.npmjs.org/@vant%2fauto-import-resolver/latest)。两个 unplugin 的当前版本来源：[components registry](https://registry.npmjs.org/unplugin-vue-components/latest) · [auto-import registry](https://registry.npmjs.org/unplugin-auto-import/latest)。仓库版本来自检索日的根 `pnpm-workspace.yaml`。

### 借鉴与拒绝照搬清单

| demo 内容 | 判断 | 理由与证据 |
| --- | --- | --- |
| `vue()` + `AutoImport({ resolvers: [VantResolver()] })` + `Components({ resolvers: [VantResolver()] })` | 借鉴配置形状 | 仍与当前 Vant 官方快速上手一致。[demo vite.config.ts](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/vite.config.ts) · [当前快速上手](https://vant-ui.github.io/vant/#/zh-CN/quickstart) |
| 模板直接使用 `<van-*>`，脚本直接调用 `showToast` | 借鉴能力验证方式 | 分别验证组件与函数 API 的自动导入；当前 resolver 仍显式支持 Toast API。[demo App.vue](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/src/App.vue) · [resolver 源码](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/src/index.ts) |
| package 版本 | 不照搬 | demo 子树自 2024-03-17 后没有更新，版本落后于 registry 和本仓库工具链。[提交历史](https://github.com/vant-ui/vant-demo/commits/master/vant/vite/) · [package.json](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/package.json) |
| `main.js` + Options API 商品页 | 不照搬 | 它是单页展示样例，不是本仓库的 TypeScript 应用骨架。[main.js](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/src/main.js) · [App.vue](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/src/App.vue) |
| `less` | 不照搬 | 只服务 demo 自己的 `<style lang="less">`；resolver 的默认按需样式入口不要求业务项目安装 Less。[App.vue](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/src/App.vue) · [resolver 源码](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/src/index.ts) |
| demo `index.html` viewport | 修正后借鉴 | demo 只有 `width=device-width, initial-scale=1.0`，缺少安全区必需的 `viewport-fit=cover`。[demo index.html](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/index.html) · [Vant 安全区文档](https://vant-ui.github.io/vant/#/zh-CN/advanced-usage) |
| `this.$router.push('cart')` | 删除 | demo package 没有 `vue-router`、`main.js` 也未安装 router，这条点击路径会在运行时失败，证明 demo 不能直接作为应用模板。[package.json](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/package.json) · [main.js](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/src/main.js) · [App.vue](https://github.com/vant-ui/vant-demo/blob/14d5d37880de7b96ab5f6b0ecfefe7fe4fc3e43b/vant/vite/src/App.vue) |

## 实施阶段建议的最小验收

1. 构建产物中不存在主动引入的 `vant/lib/index.css`，一个仅使用少数组件的空壳页面不会打入全量组件样式。[快速上手](https://vant-ui.github.io/vant/#/zh-CN/quickstart)
2. 至少用一个模板组件和一个函数 API 做构建验证，例如 `<van-button>` 与 `showToast`，从而同时覆盖 `Components` 和 `AutoImport` 两条解析路径。[快速上手](https://vant-ui.github.io/vant/#/zh-CN/quickstart) · [resolver 源码](https://github.com/youzan/vant/blob/4bdd54a52b9acc17c5645ffd05f37c0e71f5a040/packages/vant-auto-import-resolver/src/index.ts)
3. 在带 Home Indicator 的移动端视口或等效设备模拟中检查 ActionSheet 底部内容不被遮挡，并核对 `index.html` 已包含 `viewport-fit=cover`。[进阶用法](https://vant-ui.github.io/vant/#/zh-CN/advanced-usage) · [ActionSheet](https://vant-ui.github.io/vant/#/zh-CN/action-sheet)
4. 验证 ActionSheet 的“前往登录”“取消”、遮罩关闭和浏览器回退；组件默认允许点击遮罩关闭，且路由回退时自动关闭，产品行为必须明确接受或覆盖这些默认值。[ActionSheet Props](https://vant-ui.github.io/vant/#/zh-CN/action-sheet)
