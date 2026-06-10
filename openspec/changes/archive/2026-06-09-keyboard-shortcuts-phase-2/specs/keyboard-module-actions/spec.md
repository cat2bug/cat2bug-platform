## ADDED Requirements

### Requirement: 交付物页动作面板

交付物页 SHALL `registerPage('module', …)` + `page-action-hints`。

### Requirement: 交付物默认字母

| 键 | 动作 |
|----|------|
| S | 聚焦模块名查询 |
| T | 展开/折叠全部 |
| E | 新建根模块 |

### Requirement: 树表键盘导航

交付物树表 SHOULD 支持键盘导航模式（Enter 进入/Space 面板 T 后可选专用模式）：

- `↑/↓`：上/下一行
- `Enter`：编辑当前行（`handleUpdate`）
- `→`：展开节点
- `←`：折叠节点
- `Esc`：退出行焦点

#### Scenario: T 折叠全部

- **WHEN** 用户按 T
- **THEN** 切换 `toggleExpandAll` 状态

### Requirement: ModuleDialog

`ModuleDialog` SHALL 复用工具弹框套系：`Cmd/Ctrl+Enter` 保存、`Esc` 关闭、字段字母。

#### Scenario: 模块对话框保存

- **WHEN** ModuleDialog 打开且用户按 `Cmd/Ctrl+Enter`
- **THEN** 提交模块表单
