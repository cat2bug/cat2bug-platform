## 1. 后端：成员走势 userId 与缺陷域 API

- [x] 1.1 `SysMemberOfDefectsLine` 增加 `userId`；`SysDashboardMapper.xml`（MySQL + H2）resultMap 映射 `update_by_id`
- [x] 1.2 `ISysDefectStatisticService` / Impl：`defectStateLine`、`memberDefectLine` 委托 `SysDashboardService`
- [x] 1.3 `SysDefectStatisticController` 暴露 `GET defect-state-line/{projectId}`、`GET member-defect-line/{projectId}`，`system:defect:query`
- [x] 1.4 暴露 `POST .../defect-state-line/{projectId}/export`、`POST .../member-defect-line/{projectId}/export`（复用或抽取仪表盘 export 逻辑）
- [x] 1.5 单测或集成测：缺陷域 API 与 dashboard 同 projectId/timeType 计数一致；响应含 `userId`

## 2. 后端：月模式参与筛选扩展

- [x] 2.1 `DEFECT_QUERY_EXTENSION_KEYS` 注册 `participationLogMonth`（前端 `query-extension.js`）
- [x] 2.2 `SysDefectMapper.xml`（MySQL + H2）增加 `participationLogMonth` + `participationUserId` 条件
- [x] 2.3 补充 Mapper 测试：月参与筛选命中/不命中

## 3. 前端：共享图表与 API

- [x] 3.1 `api/system/statistic/defect.js` 增加 defectStateLine、memberDefectLine 及 export 方法
- [x] 3.2 新增 `Statistic/utils/defect-trend-chart.js`（compact ECharts option）；可选重构 dashboard 组件引用
- [x] 3.3 i18n 7 语言：`defect.team-defect-state-trend.*`、`defect.team-member-defect-trend.*`（及月模式文案）

## 4. 前端：TeamDefectStateTrend

- [x] 4.1 新建 `Statistic/TeamDefectStateTrend.vue`：Cat2BugCard、30天/12月、导出、115px 图区
- [x] 4.2 实现 ECharts click → `searchQuery`（defectStates + begin/endUpdateTime，`stack: false`）
- [x] 4.3 `refreshData`、主题 watch、`echarts-resize`、空态与 loading

## 5. 前端：TeamMemberDefectTrend

- [x] 5.1 新建 `Statistic/TeamMemberDefectTrend.vue`：全员折线、legend scroll、工具区
- [x] 5.2 实现 click → `participationLogDate` 或 `participationLogMonth` + `participationUserId`
- [x] 5.3 `refreshData`、主题、dispose、空态

## 6. 前端：统计条集成

- [x] 6.1 更新 `TEAM_STATISTIC_NAMES`、动态组件映射（不改 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`）
- [x] 6.2 `index.vue` 增加 `.statistic-item--TeamDefectStateTrend`、`.statistic-item--TeamMemberDefectTrend`（min-width 320px）
- [x] 6.3 `Cat2BugStatistic.refreshData` 注册两个新组件
- [x] 6.4 确认 `StatisticTemplate.vue` 团队分组可见新块

## 7. 验证

- [x] 7.1 按 `TESTING.md` 手工验收
- [x] 7.2 确认默认 7 块与用户已有模板未被自动修改；仪表盘页面回归
