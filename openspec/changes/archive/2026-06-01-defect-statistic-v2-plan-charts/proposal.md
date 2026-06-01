## Why

缺陷页统计条 v1 已提供个人待办环图与团队负载条，但缺少**个人参与趋势**与**测试计划质量**可视化；仪表盘中的测试计划燃尽图与多计划指标对比依赖 `system:dashboard:query`，且布局为全页卡片（约 350px 高），无法直接放入缺陷页 115px 统计条。v2 在保持统计条高度与权限模型的前提下，为模版自选区增加三块 ECharts 统计组件，并补齐缺陷统计域 API。

## What Changes

- 新增后端接口（挂载 `/system/defect/statistic/`）：
  - `GET .../participation/{projectId}/my?days=30` — 当前用户按缺陷日志（`sys_defect_log.create_by`）统计每日参与 distinct 缺陷数
  - `GET .../plan/list` — 当前项目测试计划下拉列表（供燃尽图选择）
  - `GET .../plan/{planId}/burndown` — 测试计划燃尽数据（与仪表盘 `planBurndown` 口径一致，日期补齐逻辑复用）
  - `GET .../plan-metrics/{projectId}` — 项目内各测试计划 6 项质量指标数值（0–100，供雷达图）
- 新增前端统计块（`Cat2BugStatistic/Statistic/`）：
  - `MyParticipationHeatmap` — 个人近 30 日日志参与热力条（115px）
  - `TeamPlanBurndown` — 团队测试计划燃尽迷你图 + 计划下拉（115px，交互参考 `PlanBurndownChart`）
  - `TeamPlanMetricsRadar` — 全项目测试计划 6 轴雷达对比（115px）
- 三块**仅出现在统计模版自选区**（`PERSONAL_STATISTIC_NAMES` / `TEAM_STATISTIC_NAMES`），**不**写入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`（已有自定义/默认 7 块用户不受影响）
- 热力图点击某日联动缺陷列表：筛选当日当前用户有日志记录的缺陷（新增列表查询参数 `params.participationLogDate`）
- ECharts 明暗主题与 v1 块一致；`refreshData` 集成

## Capabilities

### New Capabilities

- `defect-participation-heatmap`：个人缺陷日志参与按日统计 API、列表联动筛选、115px 热力条组件
- `defect-plan-statistic-charts`：测试计划燃尽/指标 API（缺陷权限域）、燃尽与雷达 115px 统计块

### Modified Capabilities

- `defect-statistic-echarts-blocks`：补充模版分组注册与「新块不进入默认模板」的明确要求

## Impact

- **后端**：`SysDefectStatisticController`、`ISysDefectStatisticService`、`SysDefectStatisticMapper`（+ XML H2/MySQL）；燃尽可委托 `ISysDashboardService.planBurndown`；计划列表/指标可委托 `ISysPlanService.selectSysPlanList`；缺陷列表 `SysDefectMapper` 增加参与日筛选
- **前端**：新增 3 个 `Statistic/*.vue`、`Cat2BugStatistic/index.vue`（分组常量 + CSS 宽度）、`api/system/statistic/defect.js`、i18n（7 语言）、`refreshData` 方法名扩展
- **权限**：统一 `system:defect:query`（不要求 `system:dashboard:query`）
- **Non-Goals**：不提高统计条默认高度；不改为 GitHub 全年历；雷达不含缺陷密度/平均修复时长；不自动向老用户模板插入新块
