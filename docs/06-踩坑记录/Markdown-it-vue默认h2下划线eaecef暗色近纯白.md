# Markdown-it-vue 默认 h2 下划线 #eaecef 暗色近纯白

- 日期：2026-10-15
- 涉及：前端

## 现象

暗色主题下 PR / Release 描述里的 `##` 标题偏大，标题下划线接近纯白，刺眼且不像项目分割线。

## 根因

全局引入的 `markdown-it-vue.css`（github markdown）写死：

`h1, h2 { border-bottom: 1px solid #eaecef }`，`h2 { font-size: 1.5em }`。

`#eaecef` 在浅色底正常，在暗色底几乎是白线。仅改组件内字色不够，下划线仍吃 github 规则。

## 解决办法

- `index.scss` 在引入后覆盖标题字号，下划线改用 `var(--divider-color)` / `--border-color-light`。
- `html.dark .markdown-body`（`theme-dark.scss`）对 `h1`/`h2` 的 `border-bottom` 再加 `!important`，压过 `#eaecef`。
- PR / Release 详情正文再收一层字号（约 14px 基线）。

## 以后怎么避免

暗色下自检 Markdown：`h1`/`h2` 下划线必须是分割线 token，禁止 `#eaecef` / `#fff`。见 [[主题与配色]]。
