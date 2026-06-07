## ADDED Requirements

### Requirement: 缺陷工具弹框直接组合键

缺陷列表工具栏操作弹框（指派、修复、驳回、通过、打开、关闭）SHALL 复用 `dialog-form-shortcuts` 与 `defect-tool-dialog-close` mixin，在弹框可见时支持：

- `Cmd/Ctrl + Enter`：提交（`onSubmit`）
- `Esc`：关闭；有未保存修改时弹出系统确认（`defect.unsaved-close-confirm`）

弹框 MUST 设置 `:close-on-press-escape="false"`，由 mixin 统一处理 `Esc`。取消按钮与 `before-close` MUST 走 `requestCloseToolDialog`。

#### Scenario: 工具弹框内提交

- **WHEN** 用户在指派弹框内按下 `Cmd/Ctrl + Enter`
- **THEN** 触发 `onSubmit` 提交逻辑

#### Scenario: 工具弹框脏数据关闭确认

- **WHEN** 用户修改指派说明后点击取消或按 `Esc`
- **THEN** 弹出未保存确认；确认后才关闭弹框

### Requirement: 工具弹框打开后聚焦首字段

工具弹框 `@opened` 时 MUST 记录关闭基线快照，并在下一帧聚焦容器内第一个逻辑 Tab 停靠点。

#### Scenario: 打开指派弹框自动聚焦

- **WHEN** 指派弹框打开完成
- **THEN** 焦点落在第一个可编辑表单项

### Requirement: 工具弹框字段徽标

工具弹框 SHALL 混入 `form-field-hints`，按住 `Cmd/Ctrl` 时在可视表单项上显示字母徽标，行为与缺陷新建/编辑抽屉一致。

#### Scenario: 指派弹框字段字母跳转

- **WHEN** 用户在指派弹框内按住 `Cmd/Ctrl` 并按下某字段字母
- **THEN** 焦点移至该字段
