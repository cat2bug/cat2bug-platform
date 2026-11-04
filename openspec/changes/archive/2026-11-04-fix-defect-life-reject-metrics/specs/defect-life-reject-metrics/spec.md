## ADDED Requirements

### Requirement: Cycle anchor from create or open

系统 MUST 将缺陷指标周期的起点定义为该缺陷 `sys_defect_log` 中类型为 `CREATE` 或 `OPEN`、且 `create_time` 最晚的一条记录的时间。若不存在此类日志，系统 MUST 将起点回退为缺陷的 `createTime`。列表与详情响应 MUST 将起点填入 `handleStartTime`。

#### Scenario: New defect uses create log as start

- **WHEN** 缺陷仅有创建日志且尚未重开
- **THEN** `handleStartTime` 等于该 `CREATE` 日志时间（或与缺陷 `createTime` 等价）

#### Scenario: Reopen starts a new cycle

- **WHEN** 缺陷在关闭或通过后执行重开并写入 `OPEN` 日志
- **THEN** `handleStartTime` 等于该次 `OPEN` 日志时间，而非更早的创建时间

### Requirement: Survival end freezes at latest pass or close

系统 MUST 将存活时间终点定义为：周期起点及之后、类型为 `PASS` 或 `CLOSED` 的日志中 `create_time` 最晚者。若本周期尚无此类日志，终点 MUST 为请求处理时的当前时刻。列表与详情响应 MUST 将终点填入 `handlePassTime`（语义为结案冻结时刻或进行中的当前时刻）。

#### Scenario: In-progress defect clock runs to now

- **WHEN** 缺陷处于处理中/待验证/已驳回/已创建等非结案态，且本周期无 `PASS`/`CLOSED` 日志
- **THEN** `handlePassTime` 为接近当前时刻的值，且按起点计算的存活时长大于 0（缺陷已存在足够久时）

#### Scenario: Pass freezes survival

- **WHEN** 本周期内存在 `PASS` 日志且无更晚的 `CLOSED`
- **THEN** `handlePassTime` 等于该次 `PASS` 时间，刷新列表时存活时长不再随墙钟增长

#### Scenario: Close freezes survival

- **WHEN** 本周期内存在 `CLOSED` 日志且无更晚的 `PASS`
- **THEN** `handlePassTime` 等于该次 `CLOSED` 时间

#### Scenario: Latest of pass and close wins

- **WHEN** 本周期内既有 `PASS` 又有更晚的 `CLOSED`（或相反）
- **THEN** `handlePassTime` 等于两者中较晚的那次日志时间

### Requirement: Reject count is cycle-scoped rejected logs

系统 MUST 将 `rejectCount` 设为本周期起点及之后、日志类型为 `REJECTED`（ordinal 4）的条数。重开进入新周期后，此前周期的驳回 MUST NOT 计入当前 `rejectCount`。系统 MUST NOT 将类型 `REJECT` 计入驳回次数。

#### Scenario: Each reject in cycle increments

- **WHEN** 本周期内对缺陷成功驳回两次并各写一条 `REJECTED` 日志
- **THEN** 列表/详情中 `rejectCount` 为 2

#### Scenario: Reopen resets reject count

- **WHEN** 缺陷在驳回若干次后关闭并重开，新周期尚未驳回
- **THEN** `rejectCount` 为 0

#### Scenario: Zero when never rejected in cycle

- **WHEN** 本周期内无 `REJECTED` 日志
- **THEN** `rejectCount` 为 0

### Requirement: List and detail expose metrics

缺陷列表与按 ID 查询详情的 API MUST 返回已计算的 `handleStartTime`、`handlePassTime`、`rejectCount`。系统 MUST NOT 要求调用方另行请求日志列表才能得到这两项指标。

#### Scenario: Table list rows include metrics

- **WHEN** 客户端请求缺陷分页列表
- **THEN** 每条缺陷记录包含可用于展示存活时间与驳回次数的上述字段

### Requirement: Frontend formats survival without zero collapse

前端存活时间格式化 MUST 使用 API 提供的 `handleStartTime` 与 `handlePassTime`。当起点缺失时 MUST 回退 `createTime`；当终点缺失时 MAY 回退当前时刻；MUST NOT 在起点与终点均缺失时同时回退为当前时刻以致恒显示 0 分钟。看板、Excel、甘特视图 MUST NOT 因本变更新增存活时间或驳回展示。

#### Scenario: Long-lived open defect shows non-zero life

- **WHEN** 用户在表格视图查看创建已超过一分钟且尚未结案的缺陷
- **THEN** 标题下「存活时间」显示非「0分钟」的时长文案

#### Scenario: Detail shows reject count

- **WHEN** 用户打开缺陷详情且本周期驳回次数为 N
- **THEN** 详情固定统计行「驳回」显示 N
