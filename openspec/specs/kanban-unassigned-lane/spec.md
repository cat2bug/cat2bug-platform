## Requirements

### Requirement: Unassigned lane to the left of status lanes

缺陷看板 MUST 在「处理中」左侧展示「未指派」泳道。缺陷 MUST 互斥分桶：当 `handleBy` 为空（`null`、非数组或长度 0）且 `defectStateName` 为 `PROCESSING`、`REJECTED` 或 `AUDIT` 时，MUST 仅出现在未指派；否则 MUST 按既有规则进入处理中 / 待验证 / 已结束。`CLOSED` 缺陷即使无处理人 MUST 仍在已结束，MUST NOT 进入未指派。未指派泳道 MUST NOT 展示泳道负责人头像条或拖放头像叠加层。

#### Scenario: Processing without handler lands in unassigned

- **WHEN** 看板数据中存在状态为处理中且 `handleBy` 为空的缺陷
- **THEN** 该卡仅出现在未指派泳道，不出现在处理中

#### Scenario: Closed without handler stays closed

- **WHEN** 存在已结束且无处理人的缺陷
- **THEN** 该卡仍在已结束泳道，未指派计数不包含它

#### Scenario: Assigned processing stays in processing

- **WHEN** 缺陷为处理中且 `handleBy` 含至少一名用户
- **THEN** 该卡在处理中，不在未指派

### Requirement: Exit-only via assign to matching status lane

用户 MUST NOT 将卡片从其他泳道拖入未指派。从未指派拖出时，系统 MUST 仅映射为 `assign`（不改变缺陷状态），且 MUST 仅允许落到与卡当前状态相符的泳道：`PROCESSING`/`REJECTED` → 处理中，`AUDIT` → 待验证。未指派拖向已结束、或拖向与状态不符的状态列时，系统 MUST 拒绝落点（禁止光标 / 不打开弹框）。允许落点且当前用户对集合内每条卡均可见 `assign` 时，系统 MUST 打开既有单条或批量指派弹框；指派成功并刷新后，卡 MUST 出现在对应状态泳道。

#### Scenario: Drag unassigned processing to processing opens assign

- **WHEN** 用户将未指派中一张处理中无人卡拖到处理中泳道松手，且具备指派权限
- **THEN** 系统打开指派弹框；成功后该卡离开未指派并出现在处理中

#### Scenario: Drag unassigned to closed is denied

- **WHEN** 用户将未指派卡片拖向已结束泳道
- **THEN** 系统显示禁止落点，不打开关闭或指派弹框，卡仍在未指派

#### Scenario: Drag unassigned audit only to audit

- **WHEN** 用户将未指派中一张待验证无人卡拖向处理中泳道
- **THEN** 系统拒绝落点；拖向待验证且有指派权限时打开指派弹框，成功后卡出现在待验证

#### Scenario: Cannot drop into unassigned

- **WHEN** 用户将处理中有处理人的卡片拖向未指派泳道松手
- **THEN** 系统拒绝落点，卡仍在原泳道，处理人不变

### Requirement: Unassigned cards draggable when assign is visible

未指派泳道内卡片对当前用户若 `assign` 动作可见，则 MUST 可拖（`draggable`）；若不可见则 MUST 呈现为不可拖（禁止光标，与既有不可拖样式策略一致）。MUST NOT 要求用户已是该缺陷处理人才能从该列拖出。

#### Scenario: Creator or permitted user can drag out

- **WHEN** 当前用户对未指派卡可见指派动作
- **THEN** 该卡可拖向相符状态列

#### Scenario: No assign permission cannot drag

- **WHEN** 当前用户对未指派卡不可见指派
- **THEN** 该卡不可拖，悬停为禁止光标
