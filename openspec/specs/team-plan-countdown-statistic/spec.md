## ADDED Requirements

### Requirement: 团队计划倒计时统计块注册

系统 SHALL 在缺陷页统计条提供名为 `TeamPlanCountdown` 的统计组件。

组件 MUST：

- 注册在 `TEAM_STATISTIC_NAMES`
- MUST NOT 加入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`
- 外层高度遵守统计条 **115px** 约束
- 在统计模版页团队分组中可选添加

#### Scenario: 默认模板不含倒计时块

- **WHEN** 用户使用默认 7 块统计模板
- **THEN** 不出现 `TeamPlanCountdown`

### Requirement: 计划选择与记忆

组件 MUST 通过 `GET /system/defect/statistic/plan/list`（或等价 `statisticPlanList`）加载当前项目测试计划列表。

标题区 MUST 提供与 `TeamPlanBurndown` 一致的计划下拉选择。

选中 `planId` MUST 写入模版项 `params.planId` 并在切换计划后刷新数据。

#### Scenario: 切换计划更新数据

- **WHEN** 用户在下拉框选择另一计划
- **THEN** 倒计时天数与四指标更新为该计划数据

#### Scenario: 无计划空状态

- **WHEN** 项目无测试计划
- **THEN** 显示空状态且不抛出未捕获异常

### Requirement: 计划数据加载

组件 MUST 通过 `GET /system/plan/{planId}`（`getPlan`）获取计划详情，至少使用字段：

- `planEndTime`
- `unexecutedCount`
- `passCount`
- `defectCount`
- `defectCloseStateCount`

若缺陷页角色无法访问 `getPlan` 且返回 403，实现 MAY 增加缺陷统计域 `GET /system/defect/statistic/plan/{planId}/summary` 返回上述字段子集，权限 MUST 为 `system:defect:query`。

#### Scenario: 加载计划成功展示指标

- **WHEN** 已选计划且 API 返回完整计划对象
- **THEN** 块内展示四指标与结束日期相关信息

### Requirement: 自然日倒计时与逾期样式

系统 MUST 按 `planEndTime` 与当前日期的 **日历日差**（本地时区）计算剩余或逾期天数。

剩余/逾期天数的 **主数字** MUST 使用电子管风格字体 **Nixie One**（`.statistic-countdown-nixie`）；逾期时 MUST 叠加 `.statistic-countdown-nixie--overdue` 红色辉光样式。单位文案（如「天」「逾期」）可使用常规字体。

- 当结束日在今天之后：展示剩余天数（正整数）
- 当结束日为今天：展示「今天」或「剩余 0 天」（实现择一，全语言 i18n 一致）
- 当结束日已过：展示「逾期 N 天」，N 为 `|diffDays|`，且样式 MUST 使用醒目红色（如 `.overdue` 或主题危险色）

未设置 `planEndTime` 时 MUST 显示明确空态或「未设置结束日期」文案。

#### Scenario: 未逾期显示剩余天数

- **WHEN** `planEndTime` 为 3 天后
- **THEN** 展示剩余 3 天且非红色逾期样式

#### Scenario: 已逾期红色提示

- **WHEN** `planEndTime` 为 2 天前
- **THEN** 展示逾期 2 天且为红色样式

### Requirement: 指标口径

展示数值 MUST 符合下列定义（不得使用其它推导字段替代）：

| 展示项 | 数据源 |
|--------|--------|
| 未完成用例 | `unexecutedCount` |
| 已完成用例 | `passCount` **仅**通过数，不含 `failCount` |
| 未解决缺陷 | `defectCount - defectCloseStateCount` |
| 已解决缺陷 | `defectCloseStateCount` |

组件 MUST NOT 将 `failCount` 计入「已完成用例」。

组件 MUST NOT 使用 `itemTotal - unexecutedCount` 或 `passCount + failCount` 作为「已完成用例」。

#### Scenario: 已完成仅含通过用例

- **WHEN** 计划 `passCount=10`、`failCount=5`、`unexecutedCount=3`
- **THEN** 「已完成用例」显示 10，而非 15 或 12

#### Scenario: 未解决缺陷计算

- **WHEN** `defectCount=20`、`defectCloseStateCount=15`
- **THEN** 「未解决缺陷」显示 5

### Requirement: 刷新与主题

`TeamPlanCountdown` MUST 实现 `refreshData()`（或等价），并被 `Cat2BugStatistic.refreshData` 调用以重新加载计划数据。

组件 MUST 在浅色与 `html.dark` 主题下可读；逾期红色在暗色主题仍须可辨认。

#### Scenario: 缺陷页刷新更新倒计时

- **WHEN** 用户触发统计条 `refreshData`
- **THEN** 倒计时块重新请求计划数据

### Requirement: 国际化

倒计时标题、天数文案、四指标标签、空状态 MUST 支持 7 种前端语言。

#### Scenario: 英文界面指标标签为英文

- **WHEN** 界面语言为 en
- **THEN** 四指标标签为英文翻译
