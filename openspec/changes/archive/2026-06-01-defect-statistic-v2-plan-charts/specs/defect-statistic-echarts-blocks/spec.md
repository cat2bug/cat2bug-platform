## MODIFIED Requirements

### Requirement: 新用户默认统计模板

当用户在当前项目下无已保存的统计模板（`listStatistic` 返回空）时，缺陷页统计条 MUST 展示以下默认块顺序：

1. `DefectMemberOnline`
2. `DefectModule`
3. `DefectState`
4. `DefectType`
5. `MyOpenTodoGauge`
6. `TeamOpenWorkloadBar`
7. `MyLife`

已有自定义模板的用户 MUST NOT 被自动覆盖。

v2 新增的 `MyParticipationHeatmap`、`TeamPlanBurndown`、`TeamPlanMetricsRadar` MUST NOT 自动插入上述默认列表；用户仅能通过 StatisticTemplate 模版自选区手动添加。

#### Scenario: 首次进入项目缺陷页

- **WHEN** 新用户首次打开某项目缺陷页且无统计模板记录
- **THEN** 统计条展示上述 7 个块，不含 v2 三个新块

#### Scenario: 已有模板不受影响

- **WHEN** 用户已保存自定义统计模板
- **THEN** 打开缺陷页仍仅展示已保存块，不自动插入 v2 块

#### Scenario: 模版自选区可添加 v2 块

- **WHEN** 用户进入 StatisticTemplate 个人或团队分组
- **THEN** 可看到并添加 `MyParticipationHeatmap`、`TeamPlanBurndown`、`TeamPlanMetricsRadar`

### Requirement: 统计模版个人与团队分组

`Cat2BugStatistic` 在 `show-type="all"` 且 `template-group` 为 `personal` 或 `team` 时，MUST 仅展示对应分组的统计块。

个人分组 MUST 包含：`MyOpenTodoGauge`、`MyLife`、`MyParticipationHeatmap`。

团队分组 MUST 包含：`DefectMemberOnline`、`DefectModule`、`DefectState`、`DefectType`、`TeamOpenWorkloadBar`、`TeamPlanBurndown`、`TeamPlanMetricsRadar`。

#### Scenario: 个人模版区展示参与热力块

- **WHEN** StatisticTemplate 渲染 `template-group="personal"`
- **THEN** 可选列表包含 `MyParticipationHeatmap`

#### Scenario: 团队模版区展示计划图表块

- **WHEN** StatisticTemplate 渲染 `template-group="team"`
- **THEN** 可选列表包含 `TeamPlanBurndown` 与 `TeamPlanMetricsRadar`
