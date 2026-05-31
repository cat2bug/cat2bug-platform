## 1. 后端：未关闭待办 API

- [x] 1.1 在 `ISysDefectStatisticService` / `SysDefectStatisticServiceImpl` 增加 `openWorkload(projectId)` 与 `openWorkloadMy(projectId, userId)`
- [x] 1.2 在 `SysDefectStatisticMapper` + XML 实现团队 Top5 与个人聚合 SQL（MySQL `JSON_CONTAINS` + H2 `REGEXP`，`defect_state != CLOSED`）
- [x] 1.3 在 `SysDefectStatisticController` 暴露 `GET /open-workload/{projectId}` 与 `GET /open-workload/{projectId}/my`，权限 `system:defect:query`
- [x] 1.4 定义响应 DTO 或 `Map` 结构（`userId`、`nickName`、`avatar`、`total`、`processing`、`audit`、`rejected`）
- [x] 1.5 补充 Service 层单测或 Mapper 集成测试（H2）：多人共管各计 1、CLOSED 不计入

## 2. 前端：API 与 i18n

- [x] 2.1 在 `api/system/statistic/defect.js` 增加 `statisticOpenWorkload(projectId)`、`statisticMyOpenWorkload(projectId)`
- [x] 2.2 增加 i18n 键：`defect.my-open-todo`、`defect.team-open-workload`（及 7 语言占位或中文+英文）

## 3. 前端：ECharts 统计块

- [x] 3.1 新建 `Statistic/MyOpenTodoGauge.vue`（环形图、115px、resize mixin、dispose）
- [x] 3.2 新建 `Statistic/TeamOpenWorkloadBar.vue`（横向条形 Top5、115px）
- [x] 3.3 实现点击 `parent.search({ handleBy, params: { defectStates } })`，`read` 模式不触发
- [x] 3.4 空数据与 loading 态（`Cat2BugCard` + `v-loading`）

## 4. 前端：统计条集成

- [x] 4.1 `Cat2BugStatistic/index.vue`：`getStatisticList` 空模板时使用默认 7 块 JSON
- [x] 4.2 增加 `.statistic-item--MyOpenTodoGauge`、`.statistic-item--TeamOpenWorkloadBar` 宽度 CSS
- [x] 4.3 暗色主题下图表色值 spot check，必要时在组件或 `theme-dark.scss` 补充变量

## 5. 验证

- [x] 5.1 手工测试见 `TESTING.md`
- [x] 5.2 确认不修改现有 DefectState/DefectType 组件行为
