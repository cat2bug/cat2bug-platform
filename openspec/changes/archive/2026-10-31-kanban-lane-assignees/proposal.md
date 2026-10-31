## Why

缺陷看板一期已支持三泳道拖拽流转，但跨列「修复」仍必须弹框选审核人，日常把卡交给某位测试时多一步。需要在处理中 / 待验证泳道提供可配置的负责人头像条，并在拖入待验证时允许砸到头像直接提交 `repair`，未砸中则保持原弹框体验。

## What Changes

- 处理中、待验证泳道头（查询框后、靠右）展示「泳道负责人」头像条；点击用现有成员多选组件增减。
- 默认勾选：处理中 → 项目「开发」角色成员；待验证 → 「测试」角色成员；该角色无成员时兜底项目管理员或项目创建人。已结束泳道不展示负责人。
- 下拉可选人 = 项目成员 ∩ 全局筛选「执行人」`queryParams.handleBy`；全局未选执行人时可选全部项目成员。
- 用户对负责人的勾选仅持久化在本机（按项目 + 泳道），不写服务端配置。
- 从其他泳道拖入**待验证**且映射动作为 `repair` 时，在目标泳道叠加可放置的负责人头像：
  - 松手命中某头像 → **不弹框**，直接 `POST .../repair`，`receiveBy` 为该成员（描述可空）；多卡同落点则串行直提同一审核人。
  - 未命中头像 → 仍打开现有 `RepairDialog` / 批量修复弹框。
- **驳回**（待验证 → 处理中）不参与头像瞄准：处理中负责人仅作泳道头配置/展示；拖入处理中一律弹框（描述必填），处理人仍由后端 `resolveRejectHandleBy` 决定。
- `close` / `pass` / `open` 等需描述或无需选人的动作行为不变；已结束无负责人叠加层。
- 无权限或不允许 `repair` 时命中头像：toast 拒绝，不调用接口、不回退弹框选人。
- 不新开后端接口与权限点；不推翻现有 ADR。独立于已完成的 `defect-list-kanban`，不回改其一期 Non-Goal 文档为「已做」，以本变更 spec 为准放宽「repair 仅弹框」条款。

## Capabilities

### New Capabilities

- `kanban-lane-assignees`: 看板泳道负责人默认与本机持久化、候选过滤、拖入待验证时头像落点直提 `repair` / 未命中弹框

### Modified Capabilities

- （无）`openspec/specs/` 尚未归档独立的 `defect-list-kanban` 主规格；本变更用新能力描述对看板交互的增量，实现时对接现有 `kanban.vue` 行为

## Impact

- 前端：`list/kanban.vue` 泳道头与拖放命中；复用 `SelectProjectMember`、项目角色/成员 API（`listProjectRole` / `listMemberOfProject` 或 `getProject`）、`repair` API、`defect-action-visibility`；本机 `localStorage`。
- 父页：读取既有 `queryParams.handleBy`（经看板 `query` prop）作候选过滤，无新筛选字段。
- 后端：无新接口；`repair` 请求体与现 DefectTools 一致。
- 文档：合入后回写 `docs/04-UI规范/组件与交互约定.md` 看板小节；`docs/08-版本记录` 当月一行。
