## ADDED Requirements

### Requirement: Shift+Cmd 布局级快捷键浮层

系统 SHALL 在用户按住 **Shift+Cmd**（Windows：**Shift+Ctrl**）时，于全局布局控件上显示导航字母徽标，字母与 `g` 导航面板共用 `buildNavItems()` 映射（可在键盘设置中自定义）。

覆盖范围 MUST 包括：

- 左侧菜单各可见页面项（字母显示在菜单项左侧）
- 顶部工具栏图标（通知、帮助、国际化、个人中心、主题、官网、源码等）
- 顶部侧栏折叠按钮（默认字母 `` ` ``）

松开 Shift 或 Cmd/Ctrl 后 MUST 立即隐藏全部布局徽标。

#### Scenario: 按住 Shift+Cmd 显示侧栏菜单字母

- **WHEN** 用户在非输入态、无遮挡层时按住 Shift+Cmd
- **THEN** 左侧可见菜单项显示对应导航字母徽标

#### Scenario: 按住 Shift+Cmd 显示折叠按钮字母

- **WHEN** 用户按住 Shift+Cmd
- **THEN** 顶部汉堡折叠按钮显示侧栏折叠快捷键字母

#### Scenario: Shift+Cmd+字母跳转页面

- **WHEN** 用户保持 Shift+Cmd 并按下某菜单项对应字母（如 `D`）
- **THEN** 路由跳转至对应页面（与 `g` 面板一致）

#### Scenario: Shift+Cmd+折叠键切换侧栏

- **WHEN** 用户保持 Shift+Cmd 并按下折叠按钮字母（默认 `` ` ``）
- **THEN** 切换侧栏展开/收起状态

### Requirement: 与页面级 Cmd 浮层互斥

仅按住 **Cmd/Ctrl**（无 Shift）时 MUST 继续触发页面级/表单级徽标（缺陷列表工具栏、抽屉字段等）。

按住 **Shift+Cmd/Ctrl** 时 MUST NOT 触发页面级或表单级徽标，避免双层提示叠加。

#### Scenario: 仅 Cmd 不显示布局字母

- **WHEN** 用户在缺陷页仅按住 Cmd（未按 Shift）
- **THEN** 显示缺陷页工具栏字母，不显示左侧菜单布局字母

#### Scenario: Shift+Cmd 不显示缺陷页工具栏字母

- **WHEN** 用户在缺陷页按住 Shift+Cmd
- **THEN** 显示布局导航字母，不显示缺陷页 `L/E/S` 等工具栏字母
