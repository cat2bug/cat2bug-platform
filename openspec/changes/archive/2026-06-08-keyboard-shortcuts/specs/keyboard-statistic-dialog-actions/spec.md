## ADDED Requirements

### Requirement: 统计配置弹框直接组合键

统计模块配置弹框（如报时提醒、我的人生）SHALL 混入 `statistic-dialog-kbd`，支持：

- `Cmd/Ctrl + Enter`：保存（`saveDialog` 等）
- `Esc` / 取消 / 关闭：未保存时确认（组件提供 `getStatisticDialogCloseSnapshot`）

#### Scenario: 报时提醒保存

- **WHEN** 用户在报时提醒弹框内按下 `Cmd/Ctrl + Enter`
- **THEN** 触发保存并关闭弹框

#### Scenario: 报时提醒未保存关闭

- **WHEN** 用户修改提醒时间后按 `Esc`
- **THEN** 弹出系统未保存确认

### Requirement: 报时提醒表格行快捷键

报时提醒弹框 SHALL 在 `remind-timer-table-kbd` 中提供表格级快捷键（与字段徽标互补）：

| 按键 | 行为 |
|---|---|
| `Cmd/Ctrl + 1`–`9` | 聚焦对应行首控件 |
| `Cmd/Ctrl + +` / `=` | 新增一行 |
| `Del` / `Backspace` | 删除当前行（名称等可编辑 input 内除外） |

#### Scenario: 数字键跳行

- **WHEN** 用户在报时提醒弹框内按住 `Cmd/Ctrl` 并按下 `3`
- **THEN** 焦点移至第 3 行第一个控件

#### Scenario: 名称框内 Del 不删行

- **WHEN** 焦点在名称输入框内且用户按下 `Del`
- **THEN** 仅删除输入字符，不删除整行

### Requirement: 报时提醒时间控件键盘

报时提醒时间选择器 MUST 处理 Element UI 时间面板挂 `body` 的焦点异常：

- 失焦时关闭时间面板
- 点「确定」或按 `Enter` 关闭面板后，焦点回到时间输入框并保留焦点环
- 面板关闭时按 `↑`/`↓` 重新打开下拉

试播、删除、新增等 `el-button--text` MUST 在 `:focus` 时显示与全站一致的焦点环。

#### Scenario: 确定后焦点留在时间框

- **WHEN** 用户打开时间下拉并点击确定
- **THEN** 下拉关闭且时间输入框仍保持聚焦

#### Scenario: 回车后按下键重开下拉

- **WHEN** 用户用 `Enter` 关闭时间下拉后按下 `↓`
- **THEN** 时间下拉重新打开
