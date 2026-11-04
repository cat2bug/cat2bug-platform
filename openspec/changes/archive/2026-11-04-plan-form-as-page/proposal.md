## Why

测试计划新建/编辑仍用 80% 宽 `AddPlanDialog`；虽已内嵌交付物树与用例多选，但弹层高度与视口受限，左右可拖布局难以对齐用例列表页的整页体验。执行工作台已改为整页（`plan-run-as-page`），新建/编辑应同一路线，便于选用例与交付物筛选，并支持可分享的表单 URL。

## What Changes

- 新增项目 namespaced 路由：`plan/new`（新建）、`plan/:planId/edit`（编辑），整页承载原对话框表单与「选用例」区。
- 列表「新建」「修改」、工作台快捷创建等入口改为 `router.push` 进入对应整页，不再打开 `AddPlanDialog`。
- 选用例区布局对齐测试用例页：左侧交付物列表（沿用 `TreePlanItemModule`）、`vue-multipane` 中缝可拖改宽、显隐与宽度缓存；查询条 + 右侧用例多选表。
- 页面左右边距与计划列表一致（外层约 20px，不叠多余内边距）。
- 保存成功后默认返回计划列表并刷新；取消/返回同样回列表。
- 主入口不再依赖 `AddPlanDialog`（可删或降为薄包装，以实现时清理为准）。

## Capabilities

### New Capabilities

- `plan-form-page`: 测试计划新建与编辑以项目内整页呈现；左侧交付物 + 可拖分栏选用例；保存/取消回到计划列表。

### Modified Capabilities

- （无）现有 `openspec/specs/` 中无计划表单展示形态的能力规格；`plan-run-as-page` 尚未归档进基线 specs。

## Impact

- 前端：`cat2bug-platform-ui` 路由 `buildPlanChildren`、`views/system/plan/index.vue`、新建整页视图（由 `AddPlanDialog` 抽离或改造）、`QuickCreate` 等入口、键盘/离开确认与 Dialog 关闭逻辑迁移。
- 后端 API 不变（仍 `POST/PUT /system/plan`，`sysPlanItemList` 按 caseId）。
- 权限仍为计划新增/修改相关权限；无权限者不进入对应页。
- 不推翻既有 ADR；交互形态与执行整页一致，不单独立 ADR（若后续多模块统一「重表单改整页」再立）。
