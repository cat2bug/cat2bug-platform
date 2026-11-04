## Why

用例页只能勾选后「发起评审」，完成后的评审批次与纪要没有可查的历史入口；用户无法按状态/时间浏览、查看统计与用例明细，也不能把已落库的纪要导出为 md/doc/pdf。需要在不改评审可选语义的前提下补齐「评审记录」浏览与导出。

## What Changes

- 用例列表工具栏「发起评审」改为 split 下拉：主按钮仍为发起评审（须勾选）；箭头下拉里含「发起评审」「评审记录」；**未勾选时主按钮禁用，箭头仍可进入评审记录**。
- 新增项目命名空间独立页：`case/reviews`（评审记录列表）、`case/reviews/:reviewId`（评审详情）；**不进侧栏菜单**，仅从下拉进入（详情可分享 URL）。
- 列表支持筛选（状态、时间范围、可选发起人）与表格展示；列表操作含「查看」、已完成记录的「导出」（md / doc / pdf）。
- 详情展示评审人、时间、统计（通过/需修改/总数等）与用例明细；进行中（`open`）可继续评审；已完成展示落库纪要。
- 导出内容为完成时写入的 **`report_markdown` 快照**（按当时团队纪要模板渲染、可含事后改稿），仅 `completed` 可导出；`open` 无纪要不可导出。与现有「保存到文档」并存。
- 后端列表接口补筛选/分页与摘要统计字段；导出优先客户端将快照转为 md/doc/pdf（复用现有 html-docx 等能力）。
- 不推翻 ADR-023（模板归属团队）与现有 `case-review` 可选评审语义；不新开 ADR。

## Capabilities

### New Capabilities

- `case-review-history`: 评审批次历史列表页、详情页、筛选、查看与按落库纪要快照导出 md/doc/pdf；用例工具栏进入入口。

### Modified Capabilities

- `case-review`: 用例页发起入口改为 split 下拉（主点发起、下拉含评审记录）；发起权限与完成写库规则不变。

## Impact

- 前端：`views/system/case/index.vue` 工具栏；新视图 `case/reviews` 与详情；`router` 中 `case` 子路由（对齐 `buildPlanChildren` 模式）；`api/system/case-review.js`；导出工具（md 直下、doc via html-docx、pdf）。
- 后端：`CaseReviewResource` / `SysCaseReviewService` / Mapper——列表查询参数与摘要字段；详情沿用现有 GET；无需改完成时模板渲染逻辑。
- 权限：列表/详情查看沿用 `system:case:query`（或现有 list/query 约定）；发起仍 `system:case:review`；导出与查看同权（仅 completed）。
- 文档：`docs/05` 接口清单、`docs/03` 纪要模板占位符补全、`docs/01` 术语/功能表按需一行；`docs/04` 若有列表入口约定可引用。
- MCP：可选后续让工作台待评审深链到 `case/reviews/:id`（本变更可不强制）。
