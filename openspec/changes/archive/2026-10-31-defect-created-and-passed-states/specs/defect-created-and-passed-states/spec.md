## ADDED Requirements

### Requirement: Created state for unassigned new defects

系统 MUST 支持缺陷状态 `CREATED`（展示名「已创建」，数字码 **5**）。创建缺陷时：若处理人为空，初始状态 MUST 为 `CREATED`；若处理人非空，初始状态 MUST 为 `PROCESSING`。`CREATED` MUST 表示尚未指派处理人；有处理人的新建 MUST NOT 使用 `CREATED`。

#### Scenario: Create without handler becomes created

- **WHEN** 用户新建缺陷且不选择处理人并提交成功
- **THEN** 该缺陷 `defectStateName` 为 `CREATED`

#### Scenario: Create with handler becomes processing

- **WHEN** 用户新建缺陷并选择至少一名处理人并提交成功
- **THEN** 该缺陷 `defectStateName` 为 `PROCESSING`

### Requirement: Pass moves to resolved as passed

验证通过动作（`pass`）MUST 将缺陷状态设为 `RESOLVED`（展示名「通过」），MUST NOT 设为 `CLOSED`。手动关闭动作（`close`）MUST 仍将状态设为 `CLOSED`（展示名「已关闭」）。系统 MUST NOT 要求「通过」后再执行关闭。

#### Scenario: Pass lands on resolved

- **WHEN** 当前用户对 `AUDIT` 缺陷执行通过
- **THEN** 缺陷状态变为 `RESOLVED`，不为 `CLOSED`

#### Scenario: Close still lands on closed

- **WHEN** 用户对活跃缺陷执行关闭
- **THEN** 缺陷状态变为 `CLOSED`

### Requirement: Assign from created requires handler and becomes processing

对 `CREATED` 缺陷执行指派时，系统 MUST 要求非空处理人，成功后状态 MUST 为 `PROCESSING`。

#### Scenario: Assign created defect

- **WHEN** 用户对 `CREATED` 缺陷完成指派并选择处理人
- **THEN** 缺陷有处理人且状态为 `PROCESSING`

### Requirement: Edit normalizes created vs processing by handler

编辑保存时：若处理人为空且状态不是 `CLOSED` 或 `RESOLVED`，系统 MUST 将状态规范为 `CREATED`；若状态为 `CREATED` 且处理人非空，系统 MUST 将状态升为 `PROCESSING`。

#### Scenario: Clear handlers on processing becomes created

- **WHEN** 用户编辑一条 `PROCESSING` 缺陷并清空处理人后保存
- **THEN** 状态变为 `CREATED`

### Requirement: Four views expose created and passed

表格、Excel、甘特、看板 MUST 能展示并筛选/区分 `CREATED`（已创建）与 `RESOLVED`（通过）。看板 MUST 为四列：已创建（仅 `CREATED`）｜处理中（`PROCESSING`+`REJECTED`）｜待验证（`AUDIT`）｜已结束（`RESOLVED`+`CLOSED`，卡片上可区分通过与关闭）。看板 MUST NOT 再以「空 handleBy 且活跃」作为单独入列条件（左列以 `CREATED` 为准）。

#### Scenario: Kanban created lane

- **WHEN** 存在状态为 `CREATED` 的缺陷并打开看板
- **THEN** 该卡出现在「已创建」列，不出现在「处理中」

#### Scenario: Kanban done lane holds pass and close

- **WHEN** 存在 `RESOLVED` 与 `CLOSED` 各一条缺陷并打开看板
- **THEN** 两卡均在「已结束」列且状态展示可区分通过与关闭

#### Scenario: Table filter includes created and resolved

- **WHEN** 用户在缺陷表按状态筛选「已创建」或「通过」
- **THEN** 列表只返回对应状态的缺陷

### Requirement: Open from resolved or closed restores active state by handler

重开（`open`）MUST 允许源状态为 `CLOSED` 或 `RESOLVED`。若重开后有处理人（请求传入或保留原处理人），状态 MUST 为 `PROCESSING`；若无处理人，状态 MUST 为 `CREATED`（看板「未指派」）。

#### Scenario: Reopen passed defect with handler

- **WHEN** 用户对有处理人的 `RESOLVED` 缺陷执行重开
- **THEN** 状态变为 `PROCESSING`

#### Scenario: Reopen closed defect without handler

- **WHEN** 用户对无处理人的 `CLOSED`（或 `RESOLVED`）缺陷执行重开且未指定处理人
- **THEN** 状态变为 `CREATED`

### Requirement: No migration of historical closed

系统 MUST NOT 因本变更批量改写既有 `CLOSED` 缺陷（含历史上由 pass 落入关闭的记录）。

#### Scenario: Old closed remains closed after upgrade

- **WHEN** 升级后读取升级前已是 `CLOSED` 的缺陷
- **THEN** 其状态仍为 `CLOSED`
