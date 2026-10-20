# 暗色主题按钮 !important 覆盖导致禁用态与可点态一样

- 日期：2026-10-19
- 涉及：后台前端（`theme-dark.scss` / Element Plus 按钮）

## 现象

暗色主题下，未勾选时「发起评审」「批量删除」等 `plain` + `:disabled` 按钮与可点时几乎同色（字仍是 `--text-color-regular`），浅色主题则明显发灰。

## 根因

`html.dark .el-button` 为对齐暗色视觉，给默认/朴素/实色按钮写了底色、边框、字色的 `!important`，并写了 `:hover` / `:focus`。这些规则**没有排除 `.is-disabled`**，盖掉了 Element Plus 自带的 `--el-button-disabled-*`，禁用态看起来仍可点。

## 解决办法

1. 写暗色按钮实色/朴素规则时，选择器加 `:not(.is-disabled)`；hover/focus 不要作用在禁用钮上。
2. 在同一块样式**后面**单独补 `.el-button.is-disabled`（含 hover/focus）的灰字、浅底、浅边，字色用 `--el-text-color-disabled` / `--text-color-placeholder`。
3. 键盘焦点相关覆盖（如 `page-kbd-hints.scss`）同样排除禁用，避免 focus 环把禁用钮刷回可点色。

## 以后怎么避免

暗色里凡对 `.el-button` 用 `!important` 改色，默认把禁用态当一等公民：要么选择器排除，要么文末再写一遍禁用。改完切暗色看未勾选工具栏按钮是否一眼发灰。
