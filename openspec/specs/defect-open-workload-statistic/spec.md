## ADDED Requirements

### Requirement: 团队未关闭待办负载 API

系统 SHALL 提供 `GET /system/defect/statistic/open-workload/{projectId}`，返回当前项目内各成员的未关闭待办数量，按 `total` 降序，默认最多 5 条。

计数规则 MUST 同时满足：

- `sys_defect.project_id` 等于路径参数
- `sys_defect.del_flag = '0'`
- `sys_defect.defect_state` 不等于 `CLOSED`
- 成员 `user_id` 包含在该缺陷的 `handle_by` JSON 数组中

当一条缺陷的 `handle_by` 包含多名成员时，系统 MUST 对该缺陷向每名成员各计 1 次。

响应 MUST 包含每条记录的 `userId`、`nickName`、`avatar`（可为空）、`total`，以及按状态拆分的 `processing`、`audit`、`rejected` 计数（与 `SysDefectStateEnum` 对应，不含 `CLOSED`）。

接口 MUST 使用权限 `system:defect:query`。

#### Scenario: 返回团队 Top 成员负载

- **WHEN** 已授权用户请求 `GET /system/defect/statistic/open-workload/{projectId}`
- **THEN** 响应为 JSON 数组，按 `total` 降序，长度不超过 5
- **THEN** 每条记录的 `total` 等于该成员满足计数规则的缺陷数之和

#### Scenario: 多人共管重复计数

- **WHEN** 项目存在 1 条未关闭缺陷且 `handle_by` 包含用户 A 与用户 B
- **THEN** 用户 A 的 `total` 计 1
- **THEN** 用户 B 的 `total` 计 1

#### Scenario: 已关闭缺陷不计入

- **WHEN** 缺陷 `defect_state` 为 `CLOSED`
- **THEN** 该缺陷 MUST NOT 计入任何成员的 `total`

### Requirement: 个人未关闭待办 API

系统 SHALL 提供 `GET /system/defect/statistic/open-workload/{projectId}/my`，返回当前登录用户在指定项目下的未关闭待办统计。

计数规则 MUST 与团队接口一致，且仅统计 `handle_by` 包含当前用户 ID 的缺陷。

响应 MUST 包含 `total`、`processing`、`audit`、`rejected` 字段。

接口 MUST 使用权限 `system:defect:query`。

#### Scenario: 返回当前用户待办汇总

- **WHEN** 已授权用户请求 `GET /system/defect/statistic/open-workload/{projectId}/my`
- **THEN** 响应为 JSON 对象，包含 `total` 及按状态拆分字段
- **THEN** `total` 等于 `processing + audit + rejected`（在数据一致时）

#### Scenario: 无待办时返回零

- **WHEN** 当前用户在项目中无未关闭待办
- **THEN** `total` 为 0，各状态字段为 0

### Requirement: H2 与 MySQL 双数据库支持

未关闭待办统计 SQL MUST 在 H2 与 MySQL 环境下均可执行，遵循项目现有 `databaseId="h2"` 双 Mapper 惯例。

#### Scenario: H2 环境查询成功

- **WHEN** 应用使用 H2 配置启动并调用团队或个人待办 API
- **THEN** 接口返回 200 与有效 JSON，无 SQL 语法错误

#### Scenario: MySQL 环境查询成功

- **WHEN** 应用使用 MySQL 配置启动并调用团队或个人待办 API
- **THEN** 接口返回 200 与有效 JSON，无 SQL 语法错误
