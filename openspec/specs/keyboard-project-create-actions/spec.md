## ADDED Requirements

### Requirement: 创建项目表单页快捷键

创建项目页（`ProjectAdd`）SHALL 混入 `project-create-form-kbd`，scope 为 `project-create`。

### Requirement: 创建项目默认字母

| 键 | 动作 |
|----|------|
| B | 返回项目列表 |
| I | 打开「更改封面」浮层 |

#### Scenario: B 返回

- **WHEN** 用户在创建项目页按 **B** 且无未保存修改
- **THEN** 返回上一页（项目列表）

#### Scenario: 未保存确认

- **WHEN** 用户已修改表单且按 **Esc** 或 **B**
- **THEN** 弹出未保存确认；确认后返回

#### Scenario: Cmd+Enter 提交

- **WHEN** 用户按 **⌘/Ctrl + Enter** 且表单校验通过
- **THEN** 调用创建接口并返回项目列表

#### Scenario: 更改封面浮层

- **WHEN** 用户按 **I** 或空格面板选择更改封面
- **THEN** 展开封面选择浮层，网格内方向键与 Enter/Space 行为与项目基本信息页一致

#### Scenario: 成员浮层打开时 Esc

- **WHEN** 「添加成员」浮层已打开
- **THEN** **Esc** 不触发返回列表

### Requirement: 与键盘设置页关系

创建项目页动作 MUST 在键盘设置页「创建项目页动作」分组可配置。

