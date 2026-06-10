## ADDED Requirements

### Requirement: 发送通知弹框组合键

发送通知弹框 SHALL 混入 `send-notice-dialog-kbd.js`，在弹框可见时支持：

- `Cmd/Ctrl + Enter`：发送（`handleSend`）
- `Esc`：关闭弹框
- 按住 `Cmd/Ctrl`：表单字段字母徽标（视口分配，与缺陷抽屉一致）

弹框 `@opened` 时 MUST 在下一帧聚焦第一个逻辑 Tab 停靠点。

#### Scenario: Esc 取消发送

- **WHEN** 用户在发送通知弹框内按下 `Esc` 且表单未修改
- **THEN** 关闭弹框（等效点击取消）

#### Scenario: Esc 关闭有未保存修改时确认

- **WHEN** 用户已修改表单后按下 `Esc`
- **THEN** 弹出未保存确认；确认后关闭弹框

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

#### Scenario: Esc 取消通知设置

- **WHEN** 用户在通知设置弹框内按下 `Esc` 且未修改配置
- **THEN** 关闭弹框（等效点击取消）

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
| `D` | 系统内部通知总开关（仅 asystem 平台 Tab；徽标在开关旁，不在标签上） |
| `E` | 开启通知音效（asystem 选项） |
| `F` | 通知音效下拉框（asystem 选项） |
| `S` | 右上角实时通知窗口（asystem 选项） |

asystem「选项」区域内 checkbox、下拉框 MUST 使用上表固定字母（不再依赖动态分配）；系统总开关行 MUST NOT 再分配表单项级字母。

切换主 Tab 或平台 Tab 后，若仍按住 `Cmd/Ctrl`，MUST 刷新徽标映射。

#### Scenario: 系统内部通知开关唯一快捷键

- **WHEN** 用户在 asystem 平台 Tab 按住 `Cmd/Ctrl`
- **THEN** 系统内部通知总开关仅显示 `D` 徽标（开关旁）
- **WHEN** 用户按下 `D`
- **THEN** 切换系统内部通知总开关

#### Scenario: 系统通知选项固定快捷键

- **WHEN** 用户在 asystem 平台 Tab 按住 `Cmd/Ctrl`
- **THEN** 「开启通知音效」显示 `E`、「音效下拉」显示 `F`、「右上角通知窗口」显示 `S`
- **WHEN** 用户按下 `E` 或 `S` 且总开关已开启
- **THEN** 切换对应勾选框
- **WHEN** 用户按下 `F` 且总开关与音效均已开启
- **THEN** 聚焦音效下拉框

#### Scenario: 外部平台单发/群发测试快捷键

- **WHEN** 用户在钉钉 / 飞书 / 企业微信平台 Tab 按住 `Cmd/Ctrl`
- **THEN** 「单发测试」「群发测试」按钮各显示一个字母徽标（在表单字段字母之后分配）
- **WHEN** 用户按下对应字母且按钮未禁用
- **THEN** 触发与点击该测试按钮相同的逻辑

#### Scenario: 每个勾选框独立快捷键

- **WHEN** 用户在「发送选项」Tab 按住 `Cmd/Ctrl`
- **THEN** 「指派给我的」「我关注的」等每个勾选框各显示一个字母徽标
- **WHEN** 用户按下某勾选框对应字母
- **THEN** 切换该勾选框选中状态

#### Scenario: 切换通知类型 Tab

- **WHEN** 用户按住 `Cmd/Ctrl` 并按下 `G`
- **THEN** 激活「通知类型」主 Tab 并刷新字段徽标

#### Scenario: 切换系统通知开关

- **WHEN** 用户在 asystem 平台 Tab 按住 `Cmd/Ctrl` 并按下 `D`
- **THEN** 切换系统通知总开关
