## ADDED Requirements

### Requirement: 用户指南键盘快捷键章节存在且可访问

系统 SHALL 在 `readme/production/advanced/` 提供名为 `keyboard-shortcuts.md` 的快捷键总览章节，并在应用内系统文档（`/system/doc`）的 **高级操作** 目录下注册该页面。**高级操作** MUST 与 **AI 指南** 为文档树同级顶级节点。

章节 MUST 使用简体中文撰写，并与现有用户指南页面（如 `user-login.md`）在结构上保持一致：含概述、分节说明与常见问题。

#### Scenario: 从系统文档树打开快捷键章节

- **WHEN** 用户登录后访问 `/system/doc` 并展开顶级节点「高级操作」
- **THEN** 用户 MUST 能看到「键盘快捷键」节点，点击后加载 `keyboard-shortcuts.md` 正文

#### Scenario: README 索引包含快捷键章节

- **WHEN** 用户阅读 `readme/production/README.md` 的「高级操作」部分
- **THEN** MUST 存在指向 `advanced/keyboard-shortcuts.md` 的链接

### Requirement: 解释三层快捷键交互模型

文档 MUST 向终端用户说明以下三种交互方式及其适用场景：

1. **引导键 + 单字母**：导航引导键（默认 `g`）打开导航面板；动作引导键（默认空格）打开当前页动作面板
2. **按住 ⌘/Ctrl**：在页面控件上显示字母徽标并可直接按键触发（页面级）
3. **按住 ⇧⌘ / Shift+Ctrl**：在侧栏菜单、顶栏工具与侧栏折叠按钮上显示字母徽标（布局级）

文档 MUST 说明 Mac 使用 ⌘、Windows/Linux 使用 Ctrl 的表述方式。

#### Scenario: 用户理解何时使用 g 与空格

- **WHEN** 用户阅读「概述」或「通用操作」节
- **THEN** 文档 MUST 明确：`g` 用于跨页面导航，空格用于当前页已注册的动作（如缺陷列表）

#### Scenario: 用户理解 ⌘ 与 ⇧⌘ 的区别

- **WHEN** 用户阅读布局级与页面级说明
- **THEN** 文档 MUST 说明 ⇧⌘ 用于侧栏与顶栏，仅 ⌘ 用于当前页面工具栏与表单字段

### Requirement: 默认键位速查表与权威来源说明

文档 MUST 提供默认键位速查表，至少覆盖：

- 侧栏菜单页面导航（缺陷、用例、测试计划、交付物、仪表盘、报告、文档、项目设置等）
- 顶栏工具（通知、系统文档、国际化、个人中心、主题、官网、源码等）及二级面板字母
- 缺陷页主要动作（新建、导入、导出、统计、翻页等）
- 登录页、注册页、通知页、系统文档页、统计模版页的主要动作字母

速查表中的默认字母 MUST 与 `cat2bug-platform-ui/src/plugins/shortcut/keymap.js` 及页面 `registerPage` 注册一致。

文档 MUST 注明：用户可在「键盘设置」页查看并修改当前生效键位；若与文档表格不一致，以设置页为准。

#### Scenario: 缺陷页默认动作字母正确

- **WHEN** 用户查阅文档中缺陷页动作表
- **THEN** 新建、导入、导出、统计、上页、下页的默认字母 MUST 分别为 E、U、R、I、B、P（或与 keymap 当前默认值一致）

#### Scenario: 导航默认字母与侧栏一致

- **WHEN** 用户查阅 `g` 导航表中「缺陷」项
- **THEN** 默认字母 MUST 为 D，对应缺陷管理路由

### Requirement: 焦点守卫与遮挡层限制说明

文档 MUST 说明在以下情形快捷键（尤其引导键与空格动作面板）通常不会触发：

- 焦点位于输入框、文本域、下拉选择或 Excel 单元格编辑态
- 已打开对话框、抽屉、消息框或 Element 浮层（如下拉菜单、日期面板）
- 使用输入法组合输入（IME）过程中

#### Scenario: 用户理解抽屉打开时空格无效

- **WHEN** 用户阅读「通用操作」或「常见问题」
- **THEN** 文档 MUST 说明打开新建/编辑缺陷抽屉后，空格不会打开页面动作面板

### Requirement: 键盘设置与自定义说明

文档 MUST 说明如何进入键盘设置（`/member/keyboard`，入口位于顶栏个人中心下拉），并描述：

- 全局启用/禁用开关
- 按分组查看与编辑键位
- 冲突检测与恢复默认
- 配置保存在浏览器 localStorage，不跨设备同步

文档标题或概述 SHOULD 包含指向键盘设置的应用内链接，格式与 `user-login.md` 一致。

#### Scenario: 用户从文档跳转到键盘设置

- **WHEN** 用户在文档页点击 `[键盘设置](/member/keyboard)` 链接
- **THEN** 应用 MUST 导航至键盘设置页（若已登录）

### Requirement: 动态字母与表单字段说明

对于表格可见行、表单字段、工具弹框字段等**动态分配**的字母徽标，文档 MUST 说明：按住 ⌘/Ctrl 时系统仅为当前可见/可用控件显示字母，而非固定全局映射。

文档 SHOULD 简要说明缺陷表单中 ⌘+Enter 保存、Esc 关闭、Tab 方向跳转等组合行为。

#### Scenario: 用户理解行号非固定字母

- **WHEN** 用户阅读缺陷列表快捷键说明
- **THEN** 文档 MUST 说明打开某行详情需按住 ⌘ 查看该行显示的字母，而非 memorizing 固定数字

### Requirement: 可选交叉引用与配图

若实施任务包含交叉引用，个人中心文档（`user-profile.md`）MAY 增加指向键盘快捷键章节的链接。

配图为可选；若提供，SHOULD 包含命令面板、页面 ⌘ 徽标或键盘设置页截图，存放于 `readme/images/user-guide/` 下并在 Markdown 中引用。

#### Scenario: 个人中心交叉引用（可选）

- **WHEN** 用户阅读个人中心文档且任务 2.3 已完成
- **THEN** 文档 MUST 包含指向 `keyboard-shortcuts.md` 的可点击链接

### Requirement: 各页面详情文档承载本页快捷键

除总览 `keyboard-shortcuts.md` 外，凡已在应用中注册页面级或弹框级快捷键的用户指南详情页，SHALL 包含 `## 键盘快捷键` 小节，写入**该页上下文**下的默认键位与操作说明。

各页小节 MUST：
- 以相对链接指向总览 `keyboard-shortcuts.md`，说明通用三层模型与守卫规则
- 提供本页默认字母表（与 `keymap.js` / `registerPage` 一致）
- 对抽屉、弹框、动态行号等场景说明「按住 ⌘/Ctrl 查看当前徽标」

总览页 MUST 提供「各页面快捷键索引」表，链接到上述详情文档的 `#键盘快捷键` 锚点。

至少覆盖：`user-login.md`、`user-register.md`、`notification-list.md`、`send-options.md`、`defect.md`、`defect-statistics.md`、Table/Excel 模式相关缺陷文档。

#### Scenario: 从缺陷介绍页跳转到本页键位

- **WHEN** 用户阅读 `defect.md` 的「键盘快捷键」节
- **THEN** MUST 能看到缺陷列表动作字母表，且文首链回总览文档

#### Scenario: 从总览索引进入登录页键位

- **WHEN** 用户在 `keyboard-shortcuts.md` 索引表点击登录页链接
- **THEN** MUST 导航至 `user-login.md#键盘快捷键` 并看到登录表单默认字母
