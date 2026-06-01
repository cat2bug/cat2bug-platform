## MODIFIED Requirements

### Requirement: 个人参与热力条统计块

系统 SHALL 在缺陷页统计条提供名为 `MyParticipationHeatmap` 的统计组件，在 **115px** 高度内展示近 **84** 日参与热力（CSS 日历网格，7 行 × 周列，非 ECharts）。

组件 MUST：

- 调用参与按日 API，请求参数 `days=84`（仍受后端 14–90 clamp 约束）
- 外层使用 `Cat2BugCard`，标题为 i18n「我的参与」（或等价键）
- 使用色块深浅表示 `count`，悬停显示自定义 tooltip（日期与次数）
- 无数据时显示空状态
- 注册在 `PERSONAL_STATISTIC_NAMES`，且 MUST NOT 加入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`

#### Scenario: 渲染 84 日热力网格

- **WHEN** 统计条包含 `MyParticipationHeatmap` 且 API 返回数据
- **THEN** 可见日历式热力网格且统计块总高度不超过 115px

#### Scenario: 点击某日联动列表

- **WHEN** 用户在缺陷页（非 `read` 模式）点击有 `count > 0` 的日期格
- **THEN** 通过 `searchQuery({ stack: false, extension: { participationLogDate, participationUserId }, common: { params: { defectStates: [], delFlag: '0' } } })` 筛选列表
- **THEN** 激活 Tab 为「全部」
- **THEN** MUST NOT 弹出「已筛选」类 Message 提示

#### Scenario: 模版预览不联动

- **WHEN** 组件处于 `read` 模式（StatisticTemplate 预览）
- **THEN** 点击格子 MUST NOT 调用 `searchQuery` 或 `parent.search`
