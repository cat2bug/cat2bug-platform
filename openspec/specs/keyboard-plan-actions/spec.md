## ADDED Requirements

### Requirement: 计划列表动作面板

测试计划页 SHALL `registerPage('plan', …)`，双通道（空格面板 + `page-action-hints`），遮挡层守卫同 Phase 1。

#### Scenario: E 创建计划

- **WHEN** 用户在一级计划列表按 E（空格或 Cmd 通道）
- **THEN** 打开 `AddPlanDialog`

### Requirement: 计划列表默认字母

| 键 | 动作 |
|----|------|
| S | 聚焦查询 |
| E | 新建计划 |
| B | 上一页 |
| P | 下一页 |

### Requirement: 计划列表行跳转

按住 `Cmd/Ctrl` 时，系统 MUST 为**当前视口内可见**的计划行在编号列分配动态字母（`1`–`9` 优先，字母池补位，与工具栏已占用字母去重）。再按对应字母 MUST 打开该计划执行（`handlePlanRun`，需 `system:plan:run` 权限）。

#### Scenario: 行数字打开执行

- **WHEN** 某计划行分配数字 2 且用户按 `Cmd/Ctrl+2`
- **THEN** 打开该计划执行抽屉

### Requirement: AddPlan 对话框

`AddPlanDialog` SHALL 使用工具弹框键盘套系；内嵌用例选择区 MUST 支持 S 聚焦用例查询。`Cmd/Ctrl+Enter` 提交计划表单。

#### Scenario: 计划对话框保存

- **WHEN** AddPlan 打开且必填项已填，用户按 `Cmd/Ctrl+Enter`
- **THEN** 提交计划

### Requirement: HandlePlan 执行抽屉

`HandlePlanDialog` SHALL：

- `Esc` 关闭（有未保存执行状态时确认）
- `Cmd/Ctrl+Enter` 提交当前可提交动作（若存在）
- 打开后聚焦首可操作控件

用例项列表方向键导航为 **SHOULD**（可分期）；首期 MUST 至少满足 Esc 与表单套系。

#### Scenario: 执行抽屉 Esc

- **WHEN** HandlePlan 抽屉打开且用户按 Esc
- **THEN** 关闭抽屉或弹出确认（若有进行中变更）

### Requirement: 查询区左右键导航

计划列表查询条 MUST 支持 **← / →** 在「计划名称 → 版本」间切换。按 **S** 进入查询导航。

#### Scenario: 查询区内左右切换

- **WHEN** 用户聚焦计划名称且处于查询导航态，按 **→**
- **THEN** 焦点移至版本输入框
