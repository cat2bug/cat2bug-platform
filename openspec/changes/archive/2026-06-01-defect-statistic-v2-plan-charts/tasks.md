## 1. 后端：个人参与 API 与列表筛选

- [x] 1.1 在 `ISysDefectStatisticService` / Impl 增加 `participationMy(projectId, userId, days)`，日期补齐与 clamp(14–90)
- [x] 1.2 在 `SysDefectStatisticMapper` + XML 实现按日 `COUNT(DISTINCT defect_id)`（MySQL + H2）
- [x] 1.3 在 `SysDefectStatisticController` 暴露 `GET /participation/{projectId}/my`
- [x] 1.4 在 `SysDefectMapper` 增加 `participationLogDate` + `participationUserId` 筛选（MySQL + H2）
- [x] 1.5 补充 Mapper/Service 测试：同日多日志计 1、补 0、参与日筛选

## 2. 后端：测试计划统计 API

- [x] 2.1 暴露 `GET /plan/list`（`selectSysPlanList`，仅 id/name 等轻量字段）
- [x] 2.2 暴露 `GET /plan/{planId}/burndown`，复用 `planBurndown` + 仪表盘日期补齐逻辑（抽取共享方法）
- [x] 2.3 暴露 `GET /plan-metrics/{projectId}`，返回 6 指标数值 0–100
- [x] 2.4 全部接口 `@PreAuthorize('system:defect:query')`
- [x] 2.5 单测：无 plan_item 时燃尽补齐、无缺陷时比率为 0

## 3. 前端：API 与 i18n

- [x] 3.1 在 `api/system/statistic/defect.js` 增加 participation、planList、planBurndown、planMetrics
- [x] 3.2 i18n 7 语言：`defect.my-participation`、`defect.team-plan-burndown`、`defect.team-plan-radar` 及 tooltip 文案

## 4. 前端：ECharts 统计块

- [x] 4.1 新建 `Statistic/MyParticipationHeatmap.vue`（30 日单行热力、115px、点击联动）
- [x] 4.2 新建 `Statistic/TeamPlanBurndown.vue`（计划下拉 + 迷你燃尽柱图）
- [x] 4.3 新建 `Statistic/TeamPlanMetricsRadar.vue`（6 轴雷达、Top5 绘制）
- [x] 4.4 明暗主题、resize mixin、dispose、空态与 loading

## 5. 前端：统计条集成

- [x] 5.1 更新 `PERSONAL_STATISTIC_NAMES`、`TEAM_STATISTIC_NAMES`（不改 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`）
- [x] 5.2 增加 `.statistic-item--MyParticipationHeatmap`、`.statistic-item--TeamPlanBurndown`、`.statistic-item--TeamPlanMetricsRadar` 宽度 CSS
- [x] 5.3 `Cat2BugStatistic.refreshData` 注册新组件刷新方法
- [x] 5.4 确认 StatisticTemplate 个人/团队分组可见新块

## 6. 验证

- [x] 6.1 按 `TESTING.md` 手工验收（实现完成，待本地运行确认）
- [x] 6.2 确认默认 7 块模板与已有用户模板未被自动修改
