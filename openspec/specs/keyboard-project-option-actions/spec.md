## ADDED Requirements

### Requirement: 项目设置枢纽页

项目设置索引页（`/project/option`）SHALL `registerPage('project-option', …)`。

### Requirement: 卡片入口快捷键

按住 `Cmd/Ctrl` 与空格动作面板 MUST 为当前**可见卡片内各功能入口**（`router-link` / 可点击链接）按 DOM 顺序注册同一套动作；字母默认见 catalog（1–9 优先，超出后补位字母）。按字母 MUST 激活对应入口（等效点击该链接/按钮）。

#### Scenario: 按 1 进入基本信息

- **WHEN** 用户有项目基本信息权限且该入口可见
- **THEN** 按 1（空格面板或 ⌘/Ctrl 通道）导航至 `project-base-info`

#### Scenario: 空格面板与 ⌘ 徽标一致

- **WHEN** 用户在项目设置枢纽页打开空格动作面板
- **THEN** 列表项与按住 ⌘/Ctrl 时卡片内显示的徽标一一对应（仅含当前可见入口）

### Requirement: 子页返回

子路由页（含 `el-page-header`）SHOULD 注册 scope `project-option`：`B` 触发 `goBack` / 路由返回；**Esc** MUST 在无遮挡浮层时触发与返回相同的逻辑（表单页含未保存确认）。

#### Scenario: 子页 Esc 返回

- **WHEN** 用户在项目 API 列表子页且未打开对话框
- **THEN** 按 **Esc** 返回项目设置枢纽

#### Scenario: 子页 Cmd+Enter 保存

- **WHEN** 用户在项目基本信息表单按 `Cmd/Ctrl+Enter`
- **THEN** 触发保存

### Requirement: 子页列表查询区导航

含「查询表单 + 右侧工具栏 + 表格 + 分页」的项目/团队设置子页（如 API KEY、项目成员、AI 账号、团队成员）MUST 接入 `list-query-keyboard-nav`：

- **S** 或焦点在查询区内时，**← / →** 在查询控件间切换；最右项 **→** 进入工具栏首按钮
- 工具栏内 **← / →** 在按钮间切换；首按钮 **←** 回到查询区最右项
- **Esc** 退出查询导航态

#### Scenario: API 页查询与工具栏切换

- **WHEN** 用户在项目 API KEY 页按 **S** 后按 **→**
- **THEN** 焦点从名称搜索框移至「新建密钥」按钮

### Requirement: 子页分页快捷键

上述列表子页在存在分页组件时 MUST 注册分页动作。因 **B** 已用于返回，上一页默认 **U**，下一页默认 **P**（与一级列表页 **B**/**P** 区分）。

#### Scenario: 成员列表翻页

- **WHEN** 项目成员列表有多页且用户按 **P**
- **THEN** 触发下一页

### Requirement: OpenAI 账号列表行动态徽标

OpenAI 账号管理列表页 MUST 在按住 `Cmd/Ctrl` 时为**当前可见行**账号名称列分配动态字母（优先 **1–9**）；按字母 MUST 触发该行 **修改**（需 `ai:account:edit` 权限）。对话框打开或加载中 MUST 不显示行徽标。

#### Scenario: 按动态字母修改账号

- **WHEN** 用户按住 **⌘/Ctrl** 且列表第 1 行显示徽标 **1**
- **THEN** 再按 **1** 打开该行账号的修改对话框

### Requirement: 缺陷自定义字段列表行动态徽标

缺陷属性管理列表页 MUST 在按住 `Cmd/Ctrl` 时为**当前可见行**中可编辑的自定义字段，在字段名称列分配动态字母（优先 **1–9**）；按字母 MUST 触发该行 **修改**（需 `system:project:defect-field:edit` 权限）。系统内置字段 MUST 不显示行徽标。对话框打开或加载中 MUST 不显示行徽标。

#### Scenario: 按动态字母修改自定义字段

- **WHEN** 用户按住 **⌘/Ctrl** 且列表第 1 个自定义字段行显示徽标 **1**
- **THEN** 再按 **1** 打开该行字段的修改对话框

### Requirement: OpenAI 账号表单弹框快捷键

新建/修改 OpenAI 账号 `el-dialog` MUST 接入 `dialog-form-shortcuts` + `form-field-hints`：**Esc** 关闭（未保存时确认）；**Cmd/Ctrl+Enter** 保存；按住 **Cmd/Ctrl** 显示字段字母徽标。

#### Scenario: 弹框 Esc 关闭

- **WHEN** 用户在新建 OpenAI 账号弹框内按 **Esc** 且未修改表单
- **THEN** 关闭弹框并回到列表

### Requirement: 项目基本信息更改封面

项目信息页（`project-base-info`）「更改封面」popover MUST 注册 **I** 打开浮层；浮层内 **↑↓←→** 在预设图标与上传项间移动；**Enter** / **Space** 选中；**Esc** 或首行 **↑** 关闭浮层。

#### Scenario: 快捷键打开封面选择

- **WHEN** 用户在项目信息页按 **I**
- **THEN** 展开封面选择浮层并聚焦首个图标

### Requirement: 项目添加成员弹框

`AddProjectMember` MUST 复用工具弹框套系：**Esc** 关闭（未保存确认）；**Cmd/Ctrl+Enter** 提交；按住 **Cmd/Ctrl** 显示字段字母徽标。弹框打开时成员列表页 **Esc** MUST 关闭弹框而非返回上级。

#### Scenario: 弹框 Esc 关闭

- **WHEN** 用户在项目添加成员弹框内按 **Esc** 且未修改表单
- **THEN** 关闭弹框并回到成员列表

### Requirement: 子页表单

`project-base-info`、项目同步、钉钉/飞书/企业微信集成等表单子页 MUST 使用 `form-field-hints` + `Cmd/Ctrl+Enter` 保存（有保存按钮时）；Esc 走未保存确认后返回。
