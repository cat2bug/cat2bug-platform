## ADDED Requirements

### Requirement: 全局键盘引擎与引导键状态机

系统 SHALL 提供一个前端全局键盘引擎，仅在登录后的应用布局内生效。引擎 MUST 支持两个引导键：导航引导键（默认 `g`）与动作引导键（默认 `空格`）。

按下引导键后引擎 MUST 进入"待接收"状态并打开对应命令面板；在该状态下引擎 MUST 仅接收单个字母键或单个数字键作为第二段输入。

待接收状态 MUST 在超时（默认 2000ms）后自动取消并关闭面板。

#### Scenario: 触发导航引导键

- **WHEN** 用户在非输入态按下 `g`
- **THEN** 引擎进入待接收状态并打开导航命令面板

#### Scenario: 引导态超时取消

- **WHEN** 进入待接收状态后超过超时时间未按第二段键
- **THEN** 引擎退出待接收状态并关闭面板，不执行任何动作

#### Scenario: 按未绑定键

- **WHEN** 待接收状态下按下未绑定的字母键
- **THEN** 不执行任何动作，并给出轻量无效提示（面板保持或关闭由实现统一）

### Requirement: 输入态焦点守卫

当焦点位于输入控件或编辑态时，引擎 MUST NOT 拦截按键，须将按键透传给页面。受保护情形 MUST 包含：`input`、`textarea`、`[contenteditable=true]`、`select`、Excel 单元格编辑态、以及 IME 组合输入中（`isComposing`）。

当焦点位于 `button`、`a` 或 `role=button|link|menuitem` 时，动作引导键（默认空格）MUST NOT 拦截。

命令面板自身打开时除外：此时引擎 MUST 接管全部相关按键。

#### Scenario: 搜索框聚焦时不拦截

- **WHEN** 用户聚焦于某搜索输入框并按下 `g`
- **THEN** 引擎不打开面板，字符正常输入到输入框

#### Scenario: IME 组合输入中不拦截

- **WHEN** 用户正在使用输入法组合输入（`event.isComposing` 为真）
- **THEN** 引擎不响应引导键

#### Scenario: 按钮聚焦时不拦截空格

- **WHEN** 用户聚焦于某 `button` 并按下空格
- **THEN** 引擎不打开页面动作面板，由按钮默认行为处理

### Requirement: 动作引导键遮挡层守卫

在已注册页面动作的前提下，按下动作引导键（默认空格）打开「页面动作」面板之前，系统 MUST 检测是否存在可见 UI 遮挡层。若存在遮挡层，MUST NOT 打开面板。

遮挡层 MUST 至少包含：`el-dialog` / `el-drawer` / `el-message-box` 包装层、命令面板遮罩、成员/交付物组合下拉、以及常见 Element 浮层（`el-select-dropdown`、`el-picker-panel`、`el-dropdown-menu` 等）。

#### Scenario: 抽屉打开时不打开页面动作

- **WHEN** 用户在缺陷页打开新建缺陷抽屉后按下空格
- **THEN** 不打开页面动作面板

#### Scenario: 成员下拉展开时不打开页面动作

- **WHEN** 用户在缺陷页展开成员选择下拉后按下空格
- **THEN** 不打开页面动作面板，空格由成员选择器用于选中当前项

#### Scenario: 一级列表无遮挡时可打开页面动作

- **WHEN** 用户在缺陷列表一级界面、无任何弹框/抽屉/浮层，且焦点不在输入框或按钮上，按下空格
- **THEN** 打开页面动作面板

### Requirement: 页面级动作注册 API

引擎 SHALL 暴露 `registerPage(scopeKey, actions)` 与 `unregisterPage(scopeKey)`，供页面声明其动作面板内容并在离开时注销。`keep-alive` 页面 MUST 通过 `activated` / `deactivated` 配对注册与注销。

动作面板内容 MUST 仅在对应页面处于激活状态时可被触发。

#### Scenario: 进入页面注册动作

- **WHEN** 缺陷页激活并调用 `registerPage('defect', [...])`
- **THEN** 动作引导键打开的面板展示缺陷页动作

#### Scenario: 离开页面注销动作

- **WHEN** 用户离开缺陷页（`deactivated` / `beforeDestroy`）
- **THEN** 缺陷页动作被注销，其他页面按下动作引导键不再展示缺陷页动作

### Requirement: 启用开关

引擎 SHALL 受全局启用开关控制。当 `enabled=false` 时，引擎 MUST NOT 拦截任何引导键。

#### Scenario: 关闭快捷键后无响应

- **WHEN** 用户在键盘设置中关闭快捷键功能
- **THEN** 按下任何引导键均无响应，所有按键透传给页面
