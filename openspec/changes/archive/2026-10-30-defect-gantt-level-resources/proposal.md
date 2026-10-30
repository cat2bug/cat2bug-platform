## Why

图层「自动排列」勾选目前只做拖动时同人重叠后推，更像防撞辅助，不像 OmniPlan Level Resources 的「一点就按资源把可排任务首尾相接、消空档、守依赖」。用户需要在保留拖动避让的同时，恢复一键全图平整（预览确认），两套能力分工清晰。

## What Changes

- **图层下拉「自动排列」动作**（在「更新基线」下，带图标；需 `system:defect:edit`）：对项目内未关闭、已排期、当前用户可改的缺陷按主处理人泳道 ASAP 挤紧（可左移、消同学道可避免空档、显式 FS 不倒序；列表自上而下为同层先后）；预览后确认再批量写回；取消不落库。
- **图层勾选改名**：现有拖动推挤开关文案改为「拖动避让」（或等价），本地键仍可用 `layers.autoLevel`；语义不变——勾选后单条拖/改期时重叠可改同伴只往后挤。
- **两套能力并存、互不强制绑定**：未勾选也可点自动排列；点自动排列不自动打开勾选；多选批量平移仍不做拖动推挤。
- **权限与空气**：与既有拖动避让一致——`project.create-by` / `project.admin` 可排全部可改候选，其他人仅自己 ∈ `handleBy`；无权改与已关闭为空气，不挡资源。
- **不做**：改处理人/交付物/工期；SS/FF/SF；工作日历；空气当硬障碍；服务端求解器；**BREAKING** 无。
- **不推翻**既有 ADR；属甘特交互扩展。

## Capabilities

### New Capabilities

- `defect-gantt-level-resources`: 一键资源平整——候选过滤、ASAP/`levelDefectPlans`、预览确认与批量写回；与拖动避让的职责边界

### Modified Capabilities

- `defect-list-gantt-view`: 工具条增加「自动排列」入口；图层勾选文案改为拖动避让语义
- `defect-list-gantt-phase2`: 明确负荷轨仍只展示；消重叠/空档由「自动排列」按钮与「拖动避让」勾选分别承担

## Impact

- 前端：`gantt.vue`（按钮、预览条/横幅、写回）、`defect-gantt.js`（复用已有 `levelDefectPlans` / 过滤 / 权限）、i18n、既有单测补预览路径夹具如需要。
- 后端：复用 `listDefect` / `updateDefect` / plan-link meta；无新接口优先。
- 文档：回写 `docs/04-UI规范/组件与交互约定.md`、`docs/08-版本记录`；无新 ADR。
- 关联变更：`defect-gantt-auto-level`（拖动避让已落地，本变更在其上补齐 OmniPlan 式一键）。
