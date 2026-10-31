## Context

看板（`list/kanban.vue`）已有三状态泳道与 `kanban-lane-assignees`（处理中/待验证负责人 + repair 头像直提）。分桶函数 `laneOf` 仅看 `defectStateName`；`resolveKanbanDropAction` 映射状态间流转；`canKanbanDragCard` 按处理人主流程判断可拖。`assign` API 已支持 PROCESSING / REJECTED / AUDIT，只改 `handleBy`。创建表单处理人可空，故「处理中 + 无人」会真实出现；产品侧认为活跃态理应有人，未指派列为收口池。

## Goals / Non-Goals

**Goals:**

- 左侧「未指派」列：活跃态且无处理人；与状态列互斥。
- 只出不进；出列仅 `assign`，落点必须与卡当前状态相符；禁拖已结束。
- 未指派无负责人条 / 无头像直提；有 `assign` 可见性则可拖。
- 复用 `AssignDialog` 与批量指派；无新后端。

**Non-Goals:**

- 拖入未指派清空处理人。
- 未指派 → 待验证/已结束一步改状态（或 assign+repair 合并）。
- 未指派泳道负责人与头像直提。
- 服务端强制「活跃态必须有处理人」校验（可后续另开）。
- 改甘特「未排期/未指派」或新 ADR。

## Decisions

1. **入列优先于状态桶**  
   `isUnassigned(d)`：`handleBy` 为 `null`/非数组/长度为 0，且状态 ∈ {PROCESSING, REJECTED, AUDIT}。  
   `laneOf`：先判未指派 → `unassigned`；否则按原状态映射。CLOSED 无人仍 `closed`。  
   *备选*：未指派仅 PROCESSING → 否决，保留 REJECTED/AUDIT 无人兜底以免「消失」。

2. **出列矩阵（窄）**  
   | from \ to | unassigned | processing | audit | closed |
   |-----------|------------|------------|-------|--------|
   | unassigned | — | assign* | assign* | 禁止 |
   | 其他 → unassigned | 禁止 | （既有） | （既有） | （既有） |

   `*` 仅当卡状态与目标列相符：PROCESSING/REJECTED→processing，AUDIT→audit。相符则 `resolveKanbanDropAction` 返回 `assign`；否则 `null` + 禁止光标。  
   *备选*：拖到任一状态列都弹 assign → 否决（松手列≠归桶列）。

3. **可拖**  
   `canKanbanDragCard`：若 `laneOf===unassigned`，则 `isDefectActionVisible('assign', …)`；其余列逻辑不变。

4. **UI**  
   泳道顺序：`unassigned, processing, audit, closed`。未指派头：标题 + 计数 + 列搜，**无**负责人（与已结束一样用等高 spacer）。拖入未指派：`isLaneDropAllowed` 恒 false。出列成功后 `search` 刷新，卡进入状态列。

5. **与负责人直提无关**  
   未指派不叠头像；不改 `kanban-lane-assignees` 的 repair 直提。

6. **空列与筛选**  
   全局 `handleBy` 有值时未指派常空——仍渲染列。列表仍前端分桶；不单开未指派接口。

## Risks / Trade-offs

- [列常空] → 接受；空态文案与其它列一致。  
- [AUDIT/REJECTED 无人罕见] → 仍入未指派，只许拖回相符列。  
- [四列变挤] → 沿用现有列宽 flex；必要时后续再做窄列/折叠（本变更不做）。  
- [无服务端强制有处理人] → 未指派持续有数据来源；不在本变更堵创建。

## Migration Plan

- 纯前端。回滚：去掉 `unassigned` 桶与相关拖放分支即可。

## Open Questions

- （无阻塞）是否在创建表单将处理人改为必填——另议，不阻塞本变更。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（缺陷看板；合入后回写四泳道与未指派拖放）
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`（既有 assign）
- `docs/07-技术决策/说明.md`：本次不新增 ADR
