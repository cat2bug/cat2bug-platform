## ADDED Requirements

### Requirement: 项目管理列表动作面板

团队项目管理页（`/team/project-list`，组件 `Project`）SHALL `registerPage('project-manage', …)` 并混入 `page-action-hints`、`list-query-keyboard-nav`。

### Requirement: 项目管理列表默认字母

| 键 | 动作 |
|----|------|
| E | 创建项目 |

#### Scenario: E 创建项目

- **WHEN** 用户有 `system:project:add` 权限且按 E
- **THEN** 跳转到创建项目页（`ProjectAdd`）

#### Scenario: 行动态徽标进入项目

- **WHEN** 用户按住 ⌘/Ctrl 且表格行对当前用户有缺陷访问权限
- **THEN** 项目名称列显示动态字母徽标；再按对应字母执行与点击项目名称相同的进入逻辑

#### Scenario: 弹框打开时不显示行徽标

- **WHEN** 列表 loading 或遗留编辑弹框 `open` 为 true
- **THEN** 不渲染表格行动态徽标

### Requirement: 与键盘设置页关系

项目管理页动作 MUST 在键盘设置页「项目管理页动作」分组可配置。
