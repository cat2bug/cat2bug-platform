## MODIFIED Requirements

### Requirement: 测试计划质量指标 API

系统 SHALL 提供 `GET /system/defect/statistic/plan-metrics/{projectId}`，返回项目内测试计划的 6 项质量指标数值。

服务实现 MUST 按 `updateTime` 降序排序后仅返回 **最多 4** 条计划（`PLAN_METRICS_TOP = 4`），而非项目下全部计划。

每项指标 MUST 为 0–100 的数值（`double`），计算规则与 `SysPlan` 的下列 getter 一致：

- 发现率（discovery）
- 修复率（repair）
- 探测率（detection）
- 严重率（severity）
- 重开率（restart）
- 逃逸率（escape）

响应每项 MUST 包含 `planId`、`planName`、`updateTime` 及上述 6 个字段。

接口 MUST 使用权限 `system:defect:query`。

#### Scenario: 多计划时仅返回 Top4

- **WHEN** 项目有 6 个测试计划且 `updateTime` 各不相同
- **THEN** 响应数组长度为 4
- **THEN** 四条记录为 `updateTime` 最新的 4 个计划

#### Scenario: 计划数不超过 4

- **WHEN** 项目有 3 个测试计划
- **THEN** 响应数组长度为 3，且每条含 6 个数值字段

#### Scenario: 无缺陷时占比为 0

- **WHEN** 某计划 `defectCount = 0`
- **THEN** 依赖缺陷数的比率字段为 0（与 `SysPlan` 字符串 `"0%"` 语义一致）

### Requirement: 测试计划燃尽统计块

系统 SHALL 在缺陷页统计条提供名为 `TeamPlanBurndown` 的统计组件，在 **115px** 内展示测试计划燃尽柱图，并支持计划下拉切换。

组件 MUST：

- 调用计划列表 API 与燃尽 API
- 标题为 i18n「测试计划燃尽」（不含当前计划名）；计划名由标题行左侧下拉展示
- 显示迷你坐标轴（与仪表盘大图可不同，但 MUST 可见刻度）
- 外层使用 `Cat2BugCard`
- 注册在 `TEAM_STATISTIC_NAMES`，且 MUST NOT 加入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`

### Requirement: 测试计划雷达统计块

系统 SHALL 在缺陷页统计条提供名为 `TeamPlanMetricsRadar` 的统计组件，在 **115px** 内用雷达图对比测试计划质量指标。

组件 MUST：

- 调用计划指标 API（已限制最多 4 条）
- 雷达轴固定为上述 6 项指标，`max` 为 100
- 绘制 API 返回的全部计划（最多 4 条多边形）
- 卡片标题 MUST 仅为 i18n「测试计划指标」，MUST NOT 在标题附加「展示前 N 个」等说明
- 雷达图左侧对齐、右侧竖向图例；图例计划名过长时截断
- Tooltip 中各指标值 MUST 以 `%` 后缀展示；Tooltip MUST `appendTo` 文档 body 并具备足够 `z-index`，避免被统计块 `overflow` 裁切
- 统计块宽度约 **200px**（`statistic-item` CSS 变量约束），雷达与图例间距紧凑
- 外层使用 `Cat2BugCard`
- 注册在 `TEAM_STATISTIC_NAMES`，且 MUST NOT 加入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`

#### Scenario: 最多 4 条计划雷达

- **WHEN** API 返回 4 个计划
- **THEN** 雷达图展示 4 条多边形且图例列出 4 项

#### Scenario: Tooltip 显示百分比

- **WHEN** 用户悬停某计划数据点
- **THEN** Tooltip 中指标值形如 `80%`，且不被统计卡片边界裁切
