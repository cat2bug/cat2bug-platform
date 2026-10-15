# 暗色主题下 --el-text-color-* 仍是浅色值导致文字看不清

- 日期：2026-10-15
- 涉及：前端

## 现象

暗色主题下，新写的组件文字是深灰色，压在深色背景上几乎看不见，例如 MCP 设置弹窗里的客户端配置示例（`<pre>` 代码块）。样式里明明写了 `color: var(--el-text-color-primary)`，看起来「已经用了 token」。这类问题反复出现。

## 根因

项目的暗色主题是自己在 `theme-dark.scss` 的 `html.dark` 里定义的一套 token（`--text-color-*`、`--bg-color-*` 等），**没有引入 Element Plus 自带的暗色变量**（`element-plus/theme-chalk/dark/css-vars.css`）。此前 `html.dark` 只重定义了 `--el-text-color-placeholder`、`--el-fill-color-*` 等少数几个 `--el-*` 变量，`--el-text-color-primary / regular / secondary` 仍是 Element 浅色默认值（`#303133` 一类）。另外 `<pre>`、`<code>` 不继承对话框的字色设置，必须自己指定。

旧规则还写着「或等价 `--el-text-color-*`」，等于引导大家用一个在暗色下没有被映射的变量。

## 解决办法

- `theme-dark.scss` 的 `html.dark` 中把 `--el-text-color-primary / regular / secondary / placeholder / disabled` 映射到项目 token。
- 暗色 `--text-color-placeholder` 由 `#4c4d4f` 改为 `#8b8d94`。
- 组件样式统一改用项目 token：文字 `--text-color-*`，代码块底色 `--bg-color-base` + 边框 `--border-color-light`。

## 以后怎么避免

- 自己写的样式只用项目 token（`--text-color-*`、`--bg-color-*`、`--border-color-*`、`--card-bg`、`--dialog-bg` 等）；`--el-*` 变量只在 `theme-dark.scss` 明确映射过的才能用。
- 需要新的 `--el-*` 变量时，先在 `html.dark` 中映射到项目 token，再在组件里用。
- `<pre>`、`<code>`、`<textarea readonly>` 等展示块必须同时写字色和底色。
- 写完切到暗色主题自检：所有文字、代码块、placeholder 一眼可读。规则见 `.cursor/rules/ui-dark-theme-contrast.mdc`，规范见 [[主题与配色]]。
