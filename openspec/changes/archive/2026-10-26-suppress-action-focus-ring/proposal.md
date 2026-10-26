## Why

鼠标点击按钮、页签、顶栏图标等动作控件后，偶发残留 Element Plus / 暗色主题的蓝色 `:focus-visible` 外框，观感像「不可控焦点框」。表单聚焦环与快捷键导航高亮仍需保留；需要把「野生 outline」与「受控焦点指示」分开。

## What Changes

- 全局抑制动作控件（`el-button`、页签、顶栏/侧栏可点击图标等）上由浏览器或 Element Plus 带来的 **残留 `:focus` / `:focus-visible` outline**（含 `theme-dark.scss` 中再次强化的 2px 蓝框）。
- **不改动**表单控件既有统一聚焦环（`--cat2bug-field-focus-*` / `cat2bug-unified-focus-ring`）。
- **不改动**快捷键导航用 class 画的高亮（如 `.list-query-toolbar-nav-focused`、`.is-keyboard-focus`、`.defect-tab-nav-focused` 等）。
- 在 `docs/04-UI规范/组件与交互约定.md` 补一条交互约定：动作点击无残留野生焦点框；键盘可见性靠导航 class / 表单聚焦环。
- 无 **BREAKING** API 变更；纯样式与文档约定。

## Capabilities

### New Capabilities

- `action-focus-ring`：动作控件点击后不得残留野生焦点外框；表单聚焦环与快捷键导航高亮保持受控可见。

### Modified Capabilities

- （无）`frontend-keyboard-a11y` 的适配层 / composable / 快捷键行为需求不变；本次只约束视觉层，不改键盘逻辑。

## Impact

- 前端样式：`cat2bug-platform-ui/src/assets/styles/cat2bug.scss`、`theme-dark.scss`，必要时 tabs / navbar / 侧栏相关局部样式。
- 文档：`docs/04-UI规范/组件与交互约定.md`（必要时交叉引用 `主题与配色.md` 聚焦环 token）。
- 不影响后端、API、构建；不推翻现有 ADR。
