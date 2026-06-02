## ADDED Requirements

### Requirement: 团队成员处理走势统计块注册

系统 SHALL 在缺陷页统计条提供名为 `TeamMemberDefectTrend` 的统计组件。

组件 MUST：

- 注册在 `TEAM_STATISTIC_NAMES`
- MUST NOT 加入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`
- 外层使用 `Cat2BugCard`，高度遵守统计条 **115px** 约束
- 块最小宽度 **320px**（与 `TeamPlanBurndown` 一致）
- 在统计模版页团队分组中可选添加

#### Scenario: 默认模板不含成员走势块

- **WHEN** 用户使用默认统计模板
- **THEN** 不出现 `TeamMemberDefectTrend`

### Requirement: 成员处理走势数据 API（缺陷统计域）

系统 SHALL 提供 `GET /system/defect/statistic/member-defect-line/{projectId}?timeType=day|month`。

实现 MUST 委托 `SysDashboardService.memberOfDefectsLine`，数据口径与 `GET /system/dashboard/{projectId}/member-defect-line` 一致：基于 `sys_defect_log` 按成员与日历日/月统计当日（或当月）处理 distinct 缺陷数。

响应 MUST 包含：

- `time`：时间轴字符串数组
- `data`：数组，每项 MUST 包含 `nickName`、`createTime`、`defectTodayCount` 及 **`userId`（Long，对应日志 create_by）**

组件 MUST 展示项目内**全部**有数据的成员系列，MUST NOT 截断为 Top N。

接口 MUST 使用权限 `system:defect:query`。

#### Scenario: 返回含 userId 的成员序列

- **WHEN** 项目内多名成员在周期内有处理记录
- **THEN** 响应 `data` 中每条记录包含非空 `userId`
- **THEN** 折线图系列数等于响应中出现的不同成员数（全员）

#### Scenario: 与仪表盘 SQL 口径一致

- **WHEN** 同一项目、同一 `timeType` 分别请求仪表盘 API 与缺陷统计域 API（成员计数按 nickName 对齐）
- **THEN** 各时间点各成员的处理数量一致

### Requirement: 时间粒度切换与导出

组件 MUST 提供 **30 天** 与 **12 个月** 切换及 Excel 导出。

导出 MUST 调用 `POST /system/defect/statistic/member-defect-line/{projectId}/export?timeType=...`，结构与仪表盘 `member-defect-line/export` 一致。

`read` 为 true 时 MUST NOT 导出。

#### Scenario: 切换时间粒度后重新加载

- **WHEN** 用户从 30 天切换到 12 个月
- **THEN** 图表按月份聚合重新渲染

### Requirement: 紧凑折线图展示（全员）

组件 MUST 以 ECharts 折线图展示**每名成员**一条系列；图例 MUST 使用 `scroll` 或等价方式以在 320px 宽度内容纳全员。

组件 MUST 支持明暗主题。

#### Scenario: 项目 5 名成员均有数据

- **WHEN** API 返回 5 个不同 `userId`
- **THEN** 图表展示 5 条折线（或 5 个可辨认系列）

### Requirement: 点击联动缺陷列表（参与日 + 成员）

用户在缺陷页（`read !== true`）点击某成员在某时间点的数据时，组件 MUST 调用 `parent.searchQuery`，且：

- `stack` 为 `false`
- `extension.participationUserId` 为该点对应 `userId`
- 日模式（`timeType=day`）：`extension.participationLogDate` 为 x 轴日期 `yyyy-MM-dd`
- 月模式（`timeType=month`）：`extension.participationLogMonth` 为 x 轴 `yyyy-MM`（须在 `DEFECT_QUERY_EXTENSION_KEYS` 注册，Mapper 按日志创建时间的年月匹配）
- `common.params.defectStates` 为空数组或等价「不限状态」
- `common.params.delFlag` 为 `'0'`

语义 MUST 与 `MyParticipationHeatmap` 点击联动一致：列出该成员在对应日/月内存在缺陷日志的缺陷。

`read` 为 true 时 MUST NOT 联动。

#### Scenario: 点击成员某日数据点

- **WHEN** 用户点击成员 A 在 `2026-05-31` 上的数据点
- **THEN** 缺陷列表仅显示成员 A 在该日有参与日志的缺陷

#### Scenario: 月模式点击

- **WHEN** `timeType=month` 且用户点击成员 A 在 `2026-05` 上的数据点
- **THEN** 缺陷列表显示成员 A 在 2026 年 5 月内有参与日志的缺陷

### Requirement: 刷新

`TeamMemberDefectTrend` MUST 实现 `refreshData()` 并注册到 `Cat2BugStatistic.refreshData`。

#### Scenario: 刷新后数据更新

- **WHEN** 统计条 `refreshData` 执行
- **THEN** 成员走势块重新请求 API

### Requirement: 国际化

标题、时间粒度、导出文案 MUST 支持 7 种前端语言；成员系列名使用 `nickName`（或回退 `userName`）。

#### Scenario: 切换语言后标题更新

- **WHEN** 用户切换界面语言
- **THEN** 卡片标题与按钮文案更新为对应翻译
