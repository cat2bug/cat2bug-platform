## 1. 分桶与拖放语义

- [x] 1.1 增加 `LANE_UNASSIGNED` 与 `isUnassigned(defect)`；`laneOf` 优先未指派，再按状态；`lanes` 顺序为未指派 → 处理中 → 待验证 → 已结束
- [x] 1.2 扩展 `resolveKanbanDropAction`：未指派 → 相符状态列返回 `assign`；未指派 → 已结束 / 不符列 / 任意 → 未指派 返回 `null`
- [x] 1.3 扩展 `canKanbanDragCard`：未指派列以 `assign` 可见性为准；其它列逻辑不变
- [x] 1.4 `isLaneDropAllowed` / 禁止光标覆盖未指派出列与禁入

## 2. UI 与弹框

- [x] 2.1 渲染未指派泳道头（标题、计数、列搜；无负责人，用既有 spacer 对齐高度）与卡片列表
- [x] 2.2 未指派出列成功时打开 `AssignDialog` / 批量指派；完成后刷新列表，卡进入状态列
- [x] 2.3 补充 i18n（泳道名等）

## 3. 回归与文档

- [x] 3.1 手工核对：处理中无人进未指派；有人进处理中；CLOSED 无人仍在已结束；只出不进；拖已结束禁止；AUDIT 无人仅可拖待验证；无 assign 权不可拖
- [x] 3.2 回写 `docs/04-UI规范/组件与交互约定.md` 看板小节；`docs/08-版本记录` 当月一行；无新 ADR / 无新踩坑则注明
