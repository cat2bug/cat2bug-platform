## ADDED Requirements

### Requirement: 用例抽屉表单集成

`AddCase.vue` 抽屉 SHALL 在打开时启用 `form-field-hints` 与 `dialog-form-shortcuts`（或等价 mixin）：`Cmd/Ctrl+Enter` 提交、`Esc` 关闭（未保存确认）、按住 `Cmd/Ctrl` 显示字段字母。字段分配 MUST 遵守 `FIELD_HINT_BLOCKED`。

#### Scenario: 抽屉内 Esc 关闭确认

- **WHEN** 用户修改用例名称后按 Esc
- **THEN** 弹出未保存确认，而非直接关闭

#### Scenario: Cmd+Enter 保存

- **WHEN** 用户在抽屉内按 `Cmd/Ctrl+Enter`
- **THEN** 触发 `submitForm`

### Requirement: AI 创建与导入对话框

`CloudCase` / `CloudCase2` 及用例导入 `el-dialog` SHALL 复用 `keyboard-tool-dialog-actions`：`Cmd/Ctrl+Enter` 确认、`Esc` 关闭、打开后聚焦首字段、字段字母徽标。

#### Scenario: 导入 dialog 键盘提交

- **WHEN** 导入对话框打开且用户按 `Cmd/Ctrl+Enter`
- **THEN** 触发导入确认（若表单有效）

### Requirement: 从用例创建缺陷抽屉

从用例行触发的 `add-defect` 抽屉 SHOULD 复用缺陷表单键盘套系（Phase 1 已实现），Phase 2 无需重复 spec，但 MUST 在用例页打开时不破坏用例页 `registerPage` 注销顺序。

#### Scenario: 缺陷抽屉打开时用例动作禁用

- **WHEN** 从用例打开新建缺陷抽屉
- **THEN** 用例页空格动作面板不打开（遮挡层守卫）
