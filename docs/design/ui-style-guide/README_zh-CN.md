# Novum UI 样式指南

[English](README.md)

这是针对 `https://www.novumaivip.com` 实测得到的移动端 UI 参考资料。它记录观察到的视觉系统，不对来源站点进行重新设计。

## 查看方式

直接在浏览器中打开 [`index.html`](index.html)，无需启动服务器或执行构建。

## 范围

- 小尺寸手机：360 x 800 CSS px，DPR 3。
- 标准手机：430 x 932 CSS px，DPR 3。
- 大尺寸手机/平板：768 x 1024 CSS px，DPR 2。
- AdsPower 配置文件 37（`k1f658vy`），Chromium 渲染引擎。
- 仅进行只读导航，未提交任何会产生变更的表单。

## 内容

- `index.html`、`styles.css`、`guide.js`：精简的可视化指南。
- `data/tokens.json`：实测设计 token 及其可信度标记。
- `data/assets.json`：已下载资源的完整清单。
- `data/summary.json`：采集范围及三种视口的摘要。
- `assets/`：83 个经过 SHA-256 去重的图片、图标和字体。

从运行时样式或资源元数据读取的值标记为 `observed`，归一化后的间距值标记为 `inferred`。未观察到的状态不会被臆造。

设备尺寸和触控输入均通过模拟实现。渲染引擎仍为 Chromium，而非 Safari/WebKit。生产内容可能在 `data/summary.json` 所记录的采集日期之后发生变化。
