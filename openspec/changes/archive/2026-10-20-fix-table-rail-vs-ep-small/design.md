## Context

`unify-list-density` 已引入表格轨 token（cell 上下 16px、单行约 56px），并在 `cat2bug.scss` 写了：

```scss
.el-table .el-table__cell { padding-top/bottom: var(--cat2bug-table-cell-padding-y); }
.el-table .el-table__body td.el-table__cell { min-height: var(--cat2bug-table-row-min-height); }
```

全站 `el-config-provider` 默认 `size=small` → 表带 `.el-table--small`。Element Plus 的 `.el-table--small .el-table__cell` 与上述选择器**同级或更具体**，级联顺序下常赢，裸表行高仍偏紧。`Cat2BugTable` 内 `:deep(tbody > .el-table__row > .el-table__cell)` 更深，所以迁过的列表正常。

用例类型页（`case-types/index.vue`）仍裸 `el-table`，工具条已 `.list-page-toolbar`，操作钮已有 green/red。

## Goals / Non-Goals

**Goals:**

- 全局 CSS 压过 `.el-table--small`，裸业务表行高跟表格轨。
- 用例类型迁 `Cat2BugTable`，操作列 `cat2bug-operate-column` + tools，与约定一致。
- 文档写明「表格轨须覆盖 EP small」。

**Non-Goals:**

- 本轮不批量迁成员 / Webhook / 缺陷字段等到 `Cat2BugTable`（CSS 已惠及行高；完整迁移另开变更）。
- 不改控件轨 32px、不改操作列 CRUD 色约定。
- 不改键盘设置等刻意 `size="small"` 的说明性小表的产品意图——若被全局抬高后观感过松，用局部 class 回退（见 Decision 3）。

## Decisions

1. **全局选择器显式包含 `--small`**  
   在 `cat2bug.scss` 将表格轨规则扩展为同时命中 `.el-table` 与 `.el-table--small`（必要时加 `!important` 或提高 specificity 到 `.el-table.el-table--small .el-table__cell`），保证 padding / body min-height 用 token。  
   - 备选：只迁用例类型、不改全局 → 其它裸表仍矮，否决。

2. **用例类型迁 Cat2BugTable**  
   列：`typeName`、`caseCount`、操作列（修改 green / 删除 red）。`columns` 本地常量即可，不必复杂缓存。工具条保持现有结构。  
   - 备选：只加 CSS、不迁组件 → 行高够了但违反「业务列表用 Cat2BugTable」约定，仍迁。

3. **刻意紧凑表的逃生舱**  
   对话框内枚举编辑表、键盘说明表等若需保持紧凑，加 class（如 `cat2bug-table-compact`）把 padding 收回 EP small 量级。首轮仅在发现回归时再挂；不预挂用例类型。

4. **文档 / ADR**  
   - 组件约定补一句：表格轨 CSS 必须覆盖 ConfigProvider `size=small` 产生的 `.el-table--small`。  
   - 拟新增踩坑：EP small 表压过表格轨 token。  
   - 拟新增 ADR：无。

## Risks / Trade-offs

- [嵌套/设置页小表一并变松] → Decision 3 局部 compact；验收扫缺陷字段枚举表、键盘页。  
- [Cat2BugTable 给简单三列表增加手柄/弹性列复杂度] → 列少时 pin/调宽可接受；若干扰可关 `pinFixedToggle` 或仅用默认列定义。  
- [与未归档的 `unify-list-density` 重叠] → 本变更只补「压过 small」缺口 + 用例类型迁移，不回滚 density。

## Migration Plan

- 纯前端。回滚：还原 `cat2bug.scss` 与 `case-types/index.vue`。

## Open Questions

- 无。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（Cat2BugTable、表格轨；拟补 EP small 覆盖）
- `docs/08-版本记录/2026-10.md`
- `openspec/changes/unify-list-density/`（上下文，未归档）
- 拟新增踩坑：EP small 表压过表格轨 token
- 拟新增 ADR：无
