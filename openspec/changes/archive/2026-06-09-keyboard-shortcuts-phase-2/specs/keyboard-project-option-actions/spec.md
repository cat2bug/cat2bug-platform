## ADDED Requirements

### Requirement: 项目设置枢纽页

项目设置索引页（`/project/option`）SHALL `registerPage('project-option', …)`。

### Requirement: 卡片数字跳转

按住 `Cmd/Ctrl` 或空格面板 MUST 为当前可见设置卡片按 DOM 顺序分配 **1–9**（跳过无权限卡片）。按数字 MUST 激活该卡片内**第一个**可见 `router-link` / 可点击入口。

#### Scenario: 按 1 进入首个卡片链接

- **WHEN** 用户有项目基本信息权限且卡片排序第一
- **THEN** 按 1 导航至 `project-base-info`

### Requirement: 子页返回

子路由页（含 `el-page-header`）SHOULD 注册 scope `project-option-sub` 或复用 B：`B` 触发 `goBack` / 路由返回。

### Requirement: 子页表单

`project-base-info`、`project-api`、`defect-fields`、第三方集成等子页表单 MUST 使用 `form-field-hints` + `Cmd/Ctrl+Enter` 保存（有保存按钮时）。

#### Scenario: 子页 Cmd+Enter 保存

- **WHEN** 用户在项目基本信息表单按 `Cmd/Ctrl+Enter`
- **THEN** 触发保存
