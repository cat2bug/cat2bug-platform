## Why

仪表盘已提供「缺陷状态变化走势」与「成员处理缺陷走势」两张折线图，但依赖 `system:dashboard:query`，且为全页大卡片布局，无法放入缺陷页 **115px** 统计条。测试人员在缺陷页需要相同口径的趋势视图，并点击图表联动下方缺陷列表，而不必跳转仪表盘。

## What Changes

- 新增团队统计块 **`TeamDefectStateTrend`**：
  - 复刻仪表盘 `DefectStateChart` 能力：30 天 / 12 个月切换、Excel 导出
  - 块宽与 `TeamPlanBurndown` 一致（**320px**），高度 **115px** 紧凑 ECharts
  - 点击折线/数据点：按 **缺陷状态 + 该点日期（或月份）的 update_time 范围** 筛选缺陷列表
- 新增团队统计块 **`TeamMemberDefectTrend`**：
  - 复刻仪表盘 `MemberOfDefectLine`：**展示项目全员**折线（不 Top N）
  - 同样 30 天 / 12 个月、导出、320px 宽
  - 点击数据点：按 **`participationLogDate` + `participationUserId`** 联动（与 `MyParticipationHeatmap` 语义一致，对应 `sys_defect_log` 口径）
- 新增缺陷统计域 API（`system:defect:query`），委托 `SysDashboardService` 同口径 SQL；成员走势响应 **必须含 `userId`**
- 注册 `TEAM_STATISTIC_NAMES`，**不**写入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`
- 抽取共享 ECharts option 构建（仪表盘与统计条 compact 模式共用，避免双份配置漂移）
- i18n 7 语言；`read` 预览不联动、不导出

## Capabilities

### New Capabilities

- `team-defect-state-trend-statistic`：缺陷状态时间序列走势、导出、点击状态+日期联动
- `team-member-defect-trend-statistic`：成员处理量时间序列走势（全员）、导出、点击成员+日期联动

### Modified Capabilities

- （无）不修改既有 `defect-plan-statistic-charts` 等行为

## Impact

- **后端**：`SysDefectStatisticController` 增加 line 查询与 export；`SysMemberOfDefectsLine` 增加 `userId`；Mapper SQL 透出 `update_by_id`
- **前端**：`TeamDefectStateTrend.vue`、`TeamMemberDefectTrend.vue`；`api/system/statistic/defect.js`；`Cat2BugStatistic` 注册与 `.statistic-item--*` 320px CSS；`refreshData`；可选 `utils/defect-trend-chart.js`
- **Non-Goals**：不改动仪表盘现有组件与权限；不改为默认 7 块；不做 Top N 成员截断；不提高统计条高度
