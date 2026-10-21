## Why

用户拖窄 `Cat2BugTable` 业务列后，列总宽常小于容器：表体按内容收缩，右侧固定操作列跟着左移，右侧出现大块空白。现有 `flex: true` 在手动调宽后会写入用户宽度并退出自动弹性，无法继续吃掉剩余空间。需要在操作列前有稳定的「弹性空列」，让操作列始终贴齐容器右缘。

## What Changes

- 在 `Cat2BugTable` **全局默认**：当检测到右侧固定操作列（`cat2bug-operate-column`）且业务列总宽小于主表可用宽度时，在操作列前注入一列**幽灵弹性列（spacer）**，宽度吃掉剩余空间。
- spacer：无标题文案、无单元格内容、不进列选择器、不写列配置缓存、不可拖序/调宽；表头高度与底边与业务列对齐。
- 列总宽 ≥ 容器（需要横滚）时 spacer 宽度为 0 或不占位；操作列继续 `fixed="right"` + 既有自动测宽（88～450）。
- 有 spacer 时，**仅 spacer 负责吃剩余宽度**，避免与业务列 `flex: true` 双弹性打架。
- 回写 `docs/04-UI规范/组件与交互约定.md`（列宽不足时的填充策略一句）与当月版本记录。

## Capabilities

### New Capabilities

- `cat2bug-table-operate-spacer`: Cat2BugTable 在右侧固定操作列前注入弹性空列，保证操作列贴齐容器右缘、剩余宽度不留白空洞。

### Modified Capabilities

- （无）`cat2bug-table-operate-column` 的自动测宽区间与测宽原则不变；本次只补操作列前的布局填充。

## Impact

- 前端：`cat2bug-platform-ui/src/components/Cat2BugTable/index.vue`（布局 pass / flex 计算 / append 前插列）；样式可能微调 wrap 或 spacer class。
- 受益页：文档、缺陷、用例、计划及所有使用 `Cat2BugTable` + 右侧 `cat2bug-operate-column` 的列表。
- 无后端 / API / **BREAKING** 变更；不推翻已有 ADR。
