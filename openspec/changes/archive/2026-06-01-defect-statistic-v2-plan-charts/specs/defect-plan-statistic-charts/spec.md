## ADDED Requirements

### Requirement: 测试计划列表 API（缺陷统计域）

系统 SHALL 提供 `GET /system/defect/statistic/plan/list`，返回当前 `projectId` 下测试计划下拉数据。

响应 MUST 为 JSON 数组，每项至少包含 `planId`、`planName`，按业务默认排序与 `selectSysPlanList` 一致。

接口 MUST 使用权限 `system:defect:query`。

#### Scenario: 返回项目计划列表

- **WHEN** 已授权用户请求 `GET /system/defect/statistic/plan/list?projectId={id}`
- **THEN** 响应包含该项目全部未删除测试计划的基本信息

### Requirement: 测试计划燃尽 API（缺陷统计域）

系统 SHALL 提供 `GET /system/defect/statistic/plan/{planId}/burndown`，返回指定测试计划的燃尽序列。

数据口径 MUST 与仪表盘 `GET /system/dashboard/{projectId}/plan/{planId}/burndown` 一致：按日汇总 `sys_plan_item` 中 `plan_item_state = 'pass'` 的数量，并 MUST 对日期轴做与仪表盘相同的空窗补齐。

响应为 `{ key: 'yyyy-MM-dd', value: number }` 数组（或等价 `SysColumnsInChart` 序列）。

接口 MUST 使用权限 `system:defect:query`。

#### Scenario: 有执行记录的计划返回序列

- **WHEN** 计划存在已更新 `sys_plan_item`
- **THEN** 响应为非空日期序列，且 `value` 为非负整数

#### Scenario: 无执行记录时仍返回补齐日期轴

- **WHEN** 计划无任何 `sys_plan_item` 更新
- **THEN** 响应仍为补齐后的日期数组（与仪表盘行为一致）

### Requirement: 测试计划质量指标 API

系统 SHALL 提供 `GET /system/defect/statistic/plan-metrics/{projectId}`，返回项目内每个测试计划的 6 项质量指标数值。

每项指标 MUST 为 0–100 的数值（`double`），计算规则与 `SysPlan` 的下列 getter 一致：

- 发现率（discovery）
- 修复率（repair）
- 探测率（detection）
- 严重率（severity）
- 重开率（restart）
- 逃逸率（escape）

响应每项 MUST 包含 `planId`、`planName` 及上述 6 个字段。

接口 MUST 使用权限 `system:defect:query`。

#### Scenario: 返回全部计划指标

- **WHEN** 项目有 3 个测试计划
- **THEN** 响应数组长度为 3，且每条含 6 个数值字段

#### Scenario: 无缺陷时占比为 0

- **WHEN** 某计划 `defectCount = 0`
- **THEN** 依赖缺陷数的比率字段为 0（与 `SysPlan` 字符串 `"0%"` 语义一致）

### Requirement: 测试计划燃尽统计块

系统 SHALL 在缺陷页统计条提供名为 `TeamPlanBurndown` 的统计组件，在 **115px** 内展示测试计划燃尽迷你图，并支持计划下拉切换。

组件 MUST：

- 调用计划列表 API 与燃尽 API
- 计划选择交互参考 `PlanBurndownChart`（标题 + 下拉显示当前计划名）
- 外层使用 `Cat2BugCard`
- 隐藏坐标轴文字，依赖 tooltip 展示日期与数值
- 注册在 `TEAM_STATISTIC_NAMES`，且 MUST NOT 加入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`

#### Scenario: 切换计划刷新图表

- **WHEN** 用户在下拉框选择另一 `planId`
- **THEN** 图表数据更新为对应计划的燃尽序列

#### Scenario: 无计划空状态

- **WHEN** 项目无测试计划
- **THEN** 组件显示空状态，不抛出未捕获错误

### Requirement: 测试计划雷达统计块

系统 SHALL 在缺陷页统计条提供名为 `TeamPlanMetricsRadar` 的统计组件，在 **115px** 内用雷达图对比测试计划质量指标。

组件 MUST：

- 调用计划指标 API
- 雷达轴固定为上述 6 项指标，`max` 为 100
- 当计划数大于 5 时，仅绘制按 `update_time` 降序的前 5 条，并在 UI 上标明计划总数
- 外层使用 `Cat2BugCard`
- 注册在 `TEAM_STATISTIC_NAMES`，且 MUST NOT 加入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`

#### Scenario: 多计划雷达展示 Top5

- **WHEN** API 返回 8 个计划
- **THEN** 雷达图最多展示 5 条多边形
- **THEN** 用户可通过 tooltip 识别各计划名称

### Requirement: 明暗主题与刷新

两个计划相关 ECharts 块与 `MyParticipationHeatmap` MUST 在浅色与 `html.dark` 主题下可读。

三个新块 MUST 实现 `refreshData()`（或等价方法），并被 `Cat2BugStatistic.refreshData` 调用。

#### Scenario: 暗色主题下图表可读

- **WHEN** 用户切换到暗色主题
- **THEN** 图表背景与文字对比充足，无白底闪块

#### Scenario: 缺陷变更后刷新

- **WHEN** 缺陷页调用 `Cat2BugStatistic.refreshData()`
- **THEN** 已挂载的三个新块重新拉取 API 数据
