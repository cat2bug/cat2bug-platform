## ADDED Requirements

### Requirement: 发送通知弹框组合键

发送通知弹框 SHALL 混入 `send-notice-dialog-kbd.js`，在弹框可见时支持：

- `Cmd/Ctrl + Enter`：发送（`handleSend`）
- `Esc`：关闭弹框
- 按住 `Cmd/Ctrl`：表单字段字母徽标（视口分配，与缺陷抽屉一致）

弹框 `@opened` 时 MUST 在下一帧聚焦第一个逻辑 Tab 停靠点。

#### Scenario: 发送通知快捷键

- **WHEN** 用户在发送通知弹框内按下 `Cmd/Ctrl + Enter`
- **THEN** 触发发送逻辑

#### Scenario: 发送弹框字段跳转

- **WHEN** 用户按住 `Cmd/Ctrl` 并按下某字段字母
- **THEN** 焦点移至对应表单项

### Requirement: 通知设置弹框组合键与关闭确认

通知设置弹框 SHALL 混入 `notice-option-dialog-kbd.js` 与 `notice-option-dialog-close.js`：

- `Cmd/Ctrl + Enter`：保存（`handleSaveOption`）
- `Esc` / 取消 / `before-close`：有未保存修改时弹出确认后再关闭

#### Scenario: 通知设置脏数据关闭

- **WHEN** 用户修改设置后按 `Esc` 或点击取消
- **THEN** 弹出未保存确认；确认后才关闭

### Requirement: 通知设置 Tab 与固定字段徽标

按住 `Cmd/Ctrl` 时，通知设置弹框 MUST 显示以下固定徽标（与动态表单字段徽标并存）：

| 徽标 | 目标 |
|---|---|
| `G` | 主 Tab「通知类型」 |
| `P` | 主 Tab「通知平台」 |
| `1` / `2` / `3…` | 平台子 Tab（系统 / 邮件 / 外部平台，按序） |
| `D` | 系统通知总开关（仅当前为 asystem 平台 Tab） |
| `F` | 背景音乐开关（仅 asystem 且总开关开启） |

切换主 Tab 或平台 Tab 后，若仍按住 `Cmd/Ctrl`，MUST 刷新徽标映射。

#### Scenario: 切换通知类型 Tab

- **WHEN** 用户按住 `Cmd/Ctrl` 并按下 `G`
- **THEN** 激活「通知类型」主 Tab 并刷新字段徽标

#### Scenario: 切换系统通知开关

- **WHEN** 用户在 asystem 平台 Tab 按住 `Cmd/Ctrl` 并按下 `D`
- **THEN** 切换系统通知总开关
