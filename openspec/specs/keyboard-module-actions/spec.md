## ADDED Requirements

### Requirement: 交付物页动作面板

交付物页 SHALL `registerPage('module', …)` + `page-action-hints`。

### Requirement: 交付物默认字母

| 键 | 动作 |
|----|------|
| F | 展开/折叠全部 |
| E | 新建根模块 |

#### Scenario: F 展开/折叠全部

- **WHEN** 用户按 F（空格或 Cmd 通道）
- **THEN** 切换 `toggleExpandAll` 状态

#### Scenario: E 新建交付物

- **WHEN** 用户有 `system:module:add` 权限且按 E
- **THEN** 打开根级新建交付物对话框

### Requirement: 树表键盘导航

交付物树表 SHOULD 支持键盘导航（焦点在树表行上时）：

- `↑/↓`：上/下一行
- `Enter`：编辑当前行（`handleUpdate`）
- `→`：展开节点
- `←`：折叠节点
- `Esc`：退出行焦点

### Requirement: ModuleDialog

`ModuleDialog` SHALL 复用工具弹框套系：`Cmd/Ctrl+Enter` 保存、`Esc` 关闭、字段字母。

#### Scenario: 模块对话框保存

- **WHEN** ModuleDialog 打开且用户按 `Cmd/Ctrl+Enter`
- **THEN** 提交模块表单
