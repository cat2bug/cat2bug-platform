## Context

`Cat2BugTable`（`cat2bug-platform-ui/src/components/Cat2BugTable/index.vue`）为缺陷、用例、计划、文档、报告等列表的共用表格壳。当前能力：

- 列顺序：Sortable.js 绑定 `thead tr`（固定区 / 普通区两个 `group`），结果写入 `cacheKey + 'defect-table-field-list'`（缺陷表键名 `defect-tabledefect-table-field-list`）。
- 显隐 / 固定：工具栏「显示字段」与表头 pin 图标（`header-left` / `header-right`）。
- 列宽：仅 `min-width`，来自 `TableOptions` 默认或 `width_xx` 多语言字段；用户调宽不落盘。
- 表头结构：`table-header` 内 `[pin][title]`，Sortable `filter` 含 `.table-header`，拖序入口不清晰且曾与 pin 点击冲突（已通过内部更新标志与点击顺序修复部分问题）。

Element UI 左固定列会复制表头 DOM（`.el-table__fixed-header-wrapper`）。表格另有自定义横向滚动条、操作列自动列宽、`syncFixedDataRowHeights` 等逻辑，新表头控件须与之兼容。

## Goals / Non-Goals

**Goals:**

- 表头 hover 显示 **拖序手柄**（pin 左侧）与 **调宽竖条**（列右缘）；未 hover 时透明且不可交互。
- **pin、标题、排序箭头** 在未 hover / hover 之间 **水平位置不变**。
- 拖序仅通过手柄触发；pin 点击切换 `fixed` 行为不变。
- 用户 `width`（px，最小 60）与 `visible`、`fixed`、列顺序一并写入现有 field-list 缓存。
- 全站 `Cat2BugTable` 消费页一次启用；暗色主题适配。

**Non-Goals:**

- Excel 视图列宽缓存（`…-excel-col-widths`）与表格列宽双向同步（第一期不做）。
- 按语言分别持久化用户列宽（第一期单一 `width` 字段，无则回退 `width_xx` / 默认）。
- 操作列、prepend 辅助列的调宽手柄。
- 将 pin 迁入「显示字段」弹层。

## Decisions

### 1. 表头控件顺序与布局稳定性

**顺序（从左到右）：** `col-handle-drag` → `table-header-pin` → `header-title` → Element 排序 `caret-wrapper` → `col-handle-resize`（叠在列右缘）。

**稳定性策略（组合）：**

| 控件 | 策略 |
|------|------|
| 拖序手柄 | **固定宽度占位**（约 18–20px），未 hover：`opacity: 0; pointer-events: none`，始终参与 flex，避免 pin 位移。 |
| pin / 标题 / 排序 | 现有 flex 流；拖序占位使 pin 左缘与「仅 pin+标题」设计基准对齐（实现时以当前仅 pin 时的 pin 左缘为验收基准）。 |
| 调宽竖条 | **`position: absolute`** 贴 `th` / `.cell` 右缘，`pointer-events` 随 hover；**不参与 flex**，避免挤压排序箭头。 |

**替代方案：** 拖序也 absolute——pin 可不预留占位，但手柄易与相邻列重叠；故拖序用占位、调宽用 absolute。

### 2. 拖序实现

- Sortable 配置 `handle: '.col-handle-drag'`，从 `filter` 中移除对整列 `.table-header` 的依赖（保留 gutter、操作列、`no-drag` 等）。
- 仍绑定主表与左固定表两个 `thead tr`，`group: 'fixed' | 'normal'` 规则不变。
- `onEnd` 沿用现有按 DOM property 重排 `tableFieldList` 逻辑。

### 3. 调宽实现

- `mousedown` on `.col-handle-resize` → `document` `mousemove`/`mouseup` 计算增量，更新对应列 `width`（clamp ≥ 60px）。
- `el-table-column` 使用 `width`（及 `min-width` 下限 60），不再仅用默认作 `min-width`。
- 更新后：`doLayout()`、`syncFixedDataRowHeights()`、`updateCustomHorizontalScrollbar()`；操作列逻辑不变。
- 持久化：**debounce 300ms** 调用 `setColumns` / `saveShowColumns`，避免拖宽高频写 localStorage。

### 4. 缓存 schema

field-list 对象数组条目扩展：

```json
{ "key", "prop", "visible", "fixed", "width": 320 }
```

- `mergeDefectTableColumnPrefs`：合并时 `width` 来自缓存，缺省用 defaults。
- `syncNewDefectTableColumnsIntoFieldListCache`：新列写入时可不带 `width`。
- `getColumnConfigForExport`：是否导出 `width` 按需扩展（导出列集仍以 visible/顺序为主）。

存储键不变；旧数据无 `width` 完全兼容。

### 5. 排除列

- `class-name` 含 `cat2bug-operate-column` 或 append 操作列：无调宽手柄；无拖序手柄若列配置 `pinFixedToggle === false` 且无自定义表头。
- prepend 列（如缺陷侧栏展开 `width=30 fixed`）：不渲染手柄。
- `pinFixedToggle === false` 的列：仅 `header-title-only`，无 pin/手柄（与现有一致）。

### 6. 固定列双表头

主表与 `.el-table__fixed-header-wrapper` 内表头 **同一套模板**；手柄样式与占位规则一致，避免固定区与主表 pin 位置视觉不一致。

### 7. 样式

- 手柄：圆角矩形（拖序）、圆角竖条（调宽）；颜色使用 `--table-header-color`、`--border-color-light` 等 token；暗色在 `theme-dark.scss` 补充。
- hover 作用域：`th:hover` 或 `.table-header:hover` 控制子手柄显隐，避免整表误触。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 调宽与 Sortable / 排序点击冲突 | 手柄 `stopPropagation`；Sortable 仅 handle 元素 |
| 固定列 colgroup 不同步 | 调宽后统一 `doLayout` + 现有 fixed 行高同步 |
| 极窄列下手柄重叠 | min-width 60px；拖序占位固定宽 |
| 缓存膨胀 | 每列仅多一个 number；debounce 写入 |
| 多语言默认宽与用户宽 | 用户 `width` 优先；切换语言不自动改用户宽（第一期） |
| Excel 与表格列宽不一致 | 文档说明；后续 change 可合并 |

## Migration Plan

- **部署**：纯前端；刷新后即生效。
- **回滚**：还原 `Cat2BugTable` 与 merge 逻辑；缓存中多余 `width` 字段无害，旧版忽略即可。
- **验收**：缺陷列表为主；用例/计划/文档/报告 smoke；暗色主题；固定列 + 横向滚动 + 操作列宽。

## Open Questions

- 无（产品已确认：pin 保留、拖序在 pin 前、min 60px、Cat2BugTable 全站一次上、布局不跳变）。
