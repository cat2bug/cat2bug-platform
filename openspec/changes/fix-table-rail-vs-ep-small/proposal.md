## Why

用例类型等仍裸用 `el-table` 的业务列表，行高看起来比缺陷/用例更矮。根因是全站 `ConfigProvider` 默认 `size=small` 带来 `.el-table--small`，其 cell padding 与全局表格轨 token（`--cat2bug-table-cell-padding-y` / `--cat2bug-table-row-min-height`）权重相当或更高，把 16px / ≈56px 压回 EP 紧凑值；`Cat2BugTable` 用更深选择器才「看起来合规」。`unify-list-density` 已声明全局应惠及所有 `el-table`，但 CSS 未压过 `--small`，用例类型又不在首批迁移名单。

## What Changes

- 加强全局表格轨样式，明确覆盖 `.el-table--small`（及默认表）的 cell 上下 padding 与 body 行 `min-height`，使裸 `el-table` 与 `Cat2BugTable` 观感同轨。
- 将**用例类型**列表迁到 `Cat2BugTable`（保留现有工具条与绿/红操作色；操作列走 `cat2bug-operate-column`）。
- 回写 `docs/04-UI规范/组件与交互约定.md`（表格轨须压过 EP small）与版本记录；可记踩坑。

## Capabilities

### New Capabilities

- `table-rail-density`: 全站业务 `el-table`（含 `--small`）单元格上下留白与单行最小高度遵循表格轨 token；用例类型列表使用 `Cat2BugTable`。

### Modified Capabilities

- （无）`unify-list-density` 产物尚未归档进主 `openspec/specs/`；本次不改其 delta，而以本 capability 固化「压过 EP small」与用例类型迁移。

## Impact

- 前端：`cat2bug.scss` 表格轨规则；`views/system/project/case-types/index.vue`。
- 副作用：其它裸业务表（如缺陷字段、成员、Webhook 等）行高也会一并抬到表格轨——符合「全局惠及」意图；嵌套小表若需刻意紧凑，可用局部 class 回退（design 约定）。
- 无后端 / API / **BREAKING**。
