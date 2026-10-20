## Context

`Cat2BugTable`（`cat2bug-platform-ui/src/components/Cat2BugTable/index.vue`）支持业务列 `flex: true`，在 `runTableLayoutPass` → `computeFlexColumnWidths` 中把「容器宽 − 非弹性列」分给弹性列。一旦用户拖宽，会把 `width` 写入列配置；`columnShouldAutoFlex` 要求「无用户宽度」，弹性失效。列总宽小于容器时，表体按内容收缩，append 的 `fixed="right"` 操作列视觉上停在数据列右侧，容器右缘留下大块空白（文档列表最易复现）。

已有操作列自动宽见 `openspec/specs/cat2bug-table-operate-column/spec.md`；表头手柄见 `cat2bug-table-header-handles`。本次只补「总宽不足时的填充」，不改测宽区间。

## Goals / Non-Goals

**Goals:**

- 凡带右侧固定操作列（`cat2bug-operate-column`）的 `Cat2BugTable`，列总宽不足时操作列贴齐容器右缘。
- 在操作列前注入幽灵弹性列（spacer）吃掉剩余宽度；表头与业务列同高、同底边对齐，无文案。
- spacer 不进列选择器、不写列缓存、不可拖序/调宽。
- 有 spacer 参与剩余分配时，**仅 spacer 吃剩余**，避免与业务 `flex` 双弹性打架。
- 列总宽溢出时 spacer 收为 0，横滚与自定义 xbar 行为不退化。

**Non-Goals:**

- 不改操作列 CRUD 色、自动测宽 88～450、表头手柄规则。
- 不要求业务页各自手写 spacer 列。
- 不改变 pin 左固定语义。
- 不新增后端或 API。

## Decisions

1. **在 Cat2BugTable 内注入 spacer，全局默认**  
   - 检测条件：存在 append 操作列（class 含 `cat2bug-operate-column`，通常 `fixed="right"`）。  
   - 实现形态：组件内部在动态业务列与 `#append` 之间渲染一列（或等价 DOM），`class-name` 含约定标识（如 `cat2bug-operate-spacer-column`），`label` 为空，`pinFixedToggle` 等价禁用（无手柄）。  
   - 备选：各业务页手写空列 → 易漏、不一致，否决。  
   - 备选：改 flex 语义让「已有用户宽的 flex 列继续吃剩余」→ 拖窄名称列会被撑回，手感差，否决。

2. **剩余宽度计算与现有 layout pass 对齐**  
   - 在 `computeFlexColumnWidths` / `runTableLayoutPass` 中：先同步操作列宽，再算  
     `remaining = mainBudget − Σ(可见业务列实宽) − prepend(selection 等)`；  
     `mainBudget` 继续用 `resolveFlexContainerWidth`（已扣操作列宽）。  
   - `remaining > 容差`（与现有 `FLEX_COLUMN_WIDTH_TOLERANCE_PX` 同量级）→ spacer 宽 = floor(remaining)；否则 spacer = 0。  
   - **有 spacer 且 remaining > 0 时**：跳过对业务 `columnShouldAutoFlex` 的剩余分配（或对业务 flex 视为本 pass 不参与），只写 spacer 宽。  
   - 备选：spacer 与业务 flex 按比例分 → 复杂且易抖，否决。

3. **spacer 不进列配置真相源**  
   - `tableFieldList` / `columns-change` / `setTableColumns` 本地缓存 MUST NOT 包含 spacer。  
   - spacer 宽仅存组件内部状态（如 `spacerColumnWidth`），窗口 resize / 调宽 / 显隐列后重算。

4. **视觉与交互**  
   - 表头：空标题；高度、底边、背景与业务列表头对齐（跟 `--table-header-bg` / 现有 th 样式）。  
   - 单元格：空；不展示手柄；`no-drag`。  
   - 暗色：不单独造色，继承表头/表体 token。

5. **文档**  
   - 回写 `docs/04-UI规范/组件与交互约定.md`：列总宽不足时由表格注入操作列前弹性空列。  
   - 拟新增 ADR：无（组件布局补强，不上升架构决策）。  
   - 踩坑：若实现中触发布局循环，对照 `docs/06-踩坑记录/Safari下表格弹性列循环重排行跳动.md`，必要时再记新坑。

## Risks / Trade-offs

- [layout pass 与 spacer 互相触发抖动] → 容差比较后再 `$set`；沿用现有 rAF / `_layoutPassRunning`；参考 Safari 弹性列踩坑。  
- [prepend 展开树列、多 fixed-left 宽度漏算] → `resolveFlexContainerWidth` / 列宽求和须计入 selection、sidebar-expand 等 unmanaged 列（现有 flex 逻辑已有 selection 分支，补齐同类）。  
- [无操作列的表格] → 不注入 spacer，行为不变。  
- [操作列未 fixed=right 的边缘用法] → 仍按「存在 operate-column」注入；若总宽不足，spacer 仍填在操作列前，避免右侧空洞。

## Migration Plan

- 纯前端；无需数据迁移。  
- 回滚：还原 `Cat2BugTable` spacer 注入与 layout 分支即可。

## Open Questions

- 无（方案 A、全局默认、表头与业务对齐已定）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（Cat2BugTable 列宽 / 操作列；拟补 spacer 策略）
- `docs/06-踩坑记录/Safari下表格弹性列循环重排行跳动.md`（布局循环风险）
- `docs/08-版本记录/2026-10.md`（归档前加一行）
- 拟新增 ADR：无
