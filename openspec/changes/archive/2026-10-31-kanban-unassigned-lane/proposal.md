## Why

缺陷看板现有三泳道按状态分桶，创建时未选处理人（或历史空 `handleBy`）的卡会混在「处理中」等列里，难以一眼收口待指派工作。需要在左侧增加「未指派」泳道，把无处理人的活跃缺陷集中出来，并通过拖到状态列完成指派。

## What Changes

- 看板最左侧新增「未指派」泳道：收录 `handleBy` 为空（`null` / 空数组）且状态为 `PROCESSING` | `REJECTED` | `AUDIT` 的缺陷；`CLOSED` 即使无人仍留在「已结束」。
- 有处理人的卡仍按状态进「处理中 / 待验证 / 已结束」；与未指派**互斥**（一张卡只出现在一列）。
- 产品上活跃态理应有处理人，未指派列主力为「处理中 + 无人」；驳回/待验证却无人仅作兜底入列。
- **只出不进**：禁止从其他泳道拖入未指派（不做清空处理人）；禁止光标与现有无权落点一致。
- 从「未指派」拖出：**只走 `assign`**（改处理人、不改状态）。允许落点：
  - 状态相符列：`PROCESSING`/`REJECTED` → 处理中；`AUDIT` → 待验证。
  - 默认主力路径：未指派 → 处理中 → 弹 `AssignDialog` / 批量指派。
  - **禁止**未指派 → 已结束；拖到与自身状态不符的状态列亦禁止（避免「松手列 ≠ 归桶列」）。
- 未指派泳道**不**展示泳道负责人头像条、**不**叠加头像直提；出列一律弹既有指派弹框。
- 未指派卡的可拖判定：对当前用户可见 `assign` 则可拖出；不沿用「须为处理人才能 repair」规则。
- 列内搜索 / 分段可见 / 标题高度占位与现有泳道对齐；无新后端接口与权限点；不推翻现有 ADR。

## Capabilities

### New Capabilities

- `kanban-unassigned-lane`: 看板未指派泳道入列规则、只出不进、拖出仅 assign 且落点须状态相符

### Modified Capabilities

- （无独立已归档主规格需改写）`defect-list-kanban` / `kanban-lane-assignees` 行为由本变更增量约束；合入后回写 `docs/04-UI规范` 看板小节

## Impact

- 前端：`list/kanban.vue` 的 `laneOf` / 分桶、`resolveKanbanDropAction`、`canKanbanDragCard`、拖放光标与 `AssignDialog` / 批量指派；i18n；标题区无负责人时的等高占位已有，未指派列同样无负责人条。
- 后端：复用既有 `assign`（已允许 PROCESSING / REJECTED / AUDIT，只改 `handleBy`）；无新 API。
- 文档：`docs/04-UI规范/组件与交互约定.md`；`docs/08-版本记录` 当月一行。
