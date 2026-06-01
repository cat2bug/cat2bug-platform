## ADDED Requirements

### Requirement: 个人缺陷日志参与按日 API

系统 SHALL 提供 `GET /system/defect/statistic/participation/{projectId}/my`，返回当前登录用户在指定项目下、按自然日聚合的缺陷参与次数。

计数规则 MUST：

- 数据源为 `sys_defect_log`，且 `create_by` 等于当前用户 ID
- 关联 `sys_defect` 且 `project_id` 等于路径参数、`del_flag = '0'`
- 每日计数为 `COUNT(DISTINCT l.defect_id)`
- 查询参数 `days` 默认 30，实现 MUST 将有效范围限制在 14–90
- 响应 MUST 对窗口内无日志的日期补 `count: 0`

响应每条记录 MUST 包含 `date`（`yyyy-MM-dd`）与 `count`（非负整数）。

接口 MUST 使用权限 `system:defect:query`。

#### Scenario: 返回 30 日参与序列

- **WHEN** 已授权用户请求 `GET /system/defect/statistic/participation/{projectId}/my?days=30`
- **THEN** 响应为 JSON 数组，长度为 30
- **THEN** 每条记录包含 `date` 与 `count`
- **THEN** 无日志的日期 `count` 为 0

#### Scenario: 同一缺陷同日多条日志计 1

- **WHEN** 用户对同一 `defect_id` 在同日写入多条 `sys_defect_log`
- **THEN** 该日 `count` 对该缺陷仅计 1

### Requirement: 参与日缺陷列表筛选

缺陷列表查询 MUST 支持按日志参与日筛选：当 `defect.params.participationLogDate` 与 `defect.params.participationUserId` 均非空时，仅返回存在满足下列条件的日志记录的缺陷：

- `sys_defect_log.defect_id` 等于该缺陷
- `sys_defect_log.create_by` 等于 `participationUserId`
- 日志 `create_time` 落在 `participationLogDate` 自然日内

H2 与 MySQL MUST 均提供等效实现。

#### Scenario: 按参与日筛选列表

- **WHEN** 列表请求携带 `params.participationLogDate=2026-06-01` 与 `params.participationUserId` 为当前用户
- **THEN** 返回缺陷均为该用户在 2026-06-01 有日志记录的去重缺陷

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

### Requirement: H2 与 MySQL 双数据库支持

参与统计与参与日筛选 SQL MUST 在 H2 与 MySQL 下均可执行，遵循项目 `databaseId="h2"` 双 Mapper 惯例。

#### Scenario: H2 环境查询成功

- **WHEN** 应用在 H2 profile 下调用参与 API 与列表筛选
- **THEN** 请求成功返回，无 SQL 语法错误
