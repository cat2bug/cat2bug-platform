## Why

测试计划列表点击名称或「执行」时，当前以 90% 宽右侧抽屉承载执行工作台；内容（统计、用例/缺陷列表、模块树、嵌套用例执行）过重，抽屉遮挡列表且不便分享深链。改为独立整页，与文档 Markdown 编辑页一致，执行时全屏专注。

## What Changes

- 新增项目 namespaced 路由 `/{teamKey}/{projectKey}/plan/:planId/run`，整页展示原执行工作台内容。
- 列表点击计划名称、「执行」、快捷键打开行：改为 `router.push` 进入执行页，不再打开右侧 `el-drawer`。
- 仪表盘等深链从「列表 + `?planId=` 自动弹抽屉」改为直达执行页；保留对旧 `?planId=` 的兼容重定向。
- 单条用例执行仍可用嵌套抽屉（`HandleCaseOfPlan`），本次不改。
- 新建/修改计划仍用 `AddPlanDialog`，本次不改。

## Capabilities

### New Capabilities

- `plan-run-page`: 测试计划执行以项目内整页呈现；列表与深链进入该页，返回回到计划列表。

### Modified Capabilities

- （无）现有 `openspec/specs/` 中无计划执行展示形态的能力规格。

## Impact

- 前端：`cat2bug-platform-ui` 路由（`buildProjectScopedChildren` / plan 子路由）、`views/system/plan/index.vue`、`components/Plan/HandlePlanDialog.vue`（或拆出整页视图）、仪表盘 `PlanStatisticsChart` 深链。
- 后端 API 不变。
- 权限仍为 `system:plan:run`；无权限者名称不可点、不进执行页。
