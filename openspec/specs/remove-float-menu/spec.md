## ADDED Requirements

### Requirement: 系统不提供全局悬浮快捷菜单

系统 MUST NOT 注册、挂载或展示 Cat2BugFloatMenu 全局悬浮快捷菜单。应用启动时 MUST NOT 向 `document.body` 注入浮动菜单 DOM，MUST NOT 暴露 `$floatMenu` API。

#### Scenario: 应用启动后无悬浮菜单 DOM

- **WHEN** 用户登录并进入任意业务页面
- **THEN** 页面中不存在 `.cat2bug-float-menu` 元素

#### Scenario: 无全局 floatMenu 插件

- **WHEN** 前端应用完成 bootstrap
- **THEN** `Vue.prototype.$floatMenu` 未定义（或插件未注册）

### Requirement: Navbar 无悬浮菜单开关

系统 SHALL 在顶部导航栏不展示悬浮菜单开关控件。

#### Scenario: 桌面端 Navbar 无 float switch

- **WHEN** 用户在桌面宽度下查看已登录页面的 Navbar
- **THEN** Navbar 不包含悬浮菜单 `el-switch` 或等价开关

### Requirement: 业务页不初始化悬浮菜单

业务页面与弹窗 MUST NOT 调用 `initFloatMenu`、`windowsInit`、`windowsDestory` 或 `$floatMenu.resetMenus` 等悬浮菜单 API。

#### Scenario: 列表页无 float menu 初始化

- **WHEN** 用户打开测试计划、缺陷、用例、文档、报告或通知列表页
- **THEN** 页面生命周期中不初始化悬浮菜单，且工具栏「新建」等按钮仍可用

#### Scenario: 弹窗关闭不重建悬浮菜单

- **WHEN** 用户关闭新建/编辑类抽屉或对话框（如新建缺陷、新建用例）
- **THEN** 系统不尝试重建或显示悬浮菜单

### Requirement: 页面内主操作路径保留

移除悬浮菜单 MUST NOT 移除页面工具栏、表格行内操作或弹窗内已有按钮；用户 MUST 仍能通过这些入口完成原悬浮菜单提供的等价操作（如新建、保存、关闭）。

#### Scenario: 测试计划新建仍可用

- **WHEN** 用户在测试计划列表页点击工具栏「新建计划」
- **THEN** 新建计划对话框正常打开
