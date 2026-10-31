## ADDED Requirements

### Requirement: Create and edit allow empty handleBy

创建与编辑缺陷时，处理人（`handleBy`）MUST 为可选。用户 MUST 能在不选择处理人的情况下成功保存；系统 MUST 将空处理人持久化为空（`null` 或空数组），且后续读取 MUST 视为无处理人。缺陷名称等其它既有必填规则 MUST NOT 因本变更放宽。

#### Scenario: Create defect without handler

- **WHEN** 用户新建缺陷，填写必填的缺陷名称等字段且不选择处理人并提交
- **THEN** 创建成功，该缺陷无处理人

#### Scenario: Edit clear or leave handler empty

- **WHEN** 用户编辑缺陷并将处理人清空后保存（或保存时本就无人）
- **THEN** 保存成功，该缺陷无处理人

#### Scenario: Create with handler still works

- **WHEN** 用户新建缺陷并选择至少一名处理人后提交
- **THEN** 创建成功，`handleBy` 包含所选用户

### Requirement: Builtin handleBy is always shown but not required

内置字段目录中 `handleBy` MUST 保持始终启用（不可被团队关闭展示），但其 `required` MUST 为非必填。字段管理与表单布局 MUST 仍展示处理人控件，MUST NOT 再将其标为表单必填。

#### Scenario: Form shows handler without required validation

- **WHEN** 用户打开新建或编辑缺陷表单
- **THEN** 处理人字段可见，提交前不因未选处理人被表单校验拦截

### Requirement: Excel import allows empty handler

缺陷 Excel 模板与导入中，处理人列 MUST 为非必填。处理人为空的行在其它必填字段合法时 MUST 允许导入为无处理人缺陷。

#### Scenario: Import row without handler name

- **WHEN** 用户导入一行合法缺陷数据且处理人单元格为空
- **THEN** 该行导入成功且缺陷无处理人

### Requirement: List header not marked required for handleBy

缺陷主列表处理人列默认配置 MUST NOT 设置 `required: true`（表头 MUST NOT 因处理人显示必填红标）。缺陷名称等仍为必填的列 MUST 保持红标规则不变。

#### Scenario: Handle-by column header not red-required

- **WHEN** 用户查看缺陷表格视图表头
- **THEN** 处理人列表头不按必填样式标红，缺陷名称列表头仍按必填标红

### Requirement: Transition action dialogs still require people

单条与批量的指派（`assign`）弹框中，处理人 MUST 仍必填。修复/提测（`repair`）弹框中需选择的审核人（或等价下一处理人字段）MUST 仍必填。本变更 MUST NOT 放宽上述动作弹框的选人校验。

#### Scenario: Assign dialog rejects empty handler

- **WHEN** 用户打开指派弹框且未选择处理人就提交
- **THEN** 校验拦截，不调用指派接口

#### Scenario: Repair dialog rejects empty reviewer

- **WHEN** 用户打开修复/提测弹框且未选择所需审核人就提交
- **THEN** 校验拦截，不调用修复接口

### Requirement: Unassigned kanban lane receives empty-handler active defects

无处理人且状态为 `PROCESSING`、`REJECTED` 或 `AUDIT` 的缺陷 MUST 出现在看板「未指派」泳道（与既有 `kanban-unassigned-lane` 分桶一致）。新建时未选处理人且初始为处理中的缺陷，在看板刷新后 MUST 落入未指派。

#### Scenario: New defect without handler lands in unassigned

- **WHEN** 用户不选处理人创建缺陷且缺陷处于处理中，并打开看板视图
- **THEN** 该卡出现在「未指派」泳道，不出现在「处理中」
