## ADDED Requirements

### Requirement: 浏览器与操作系统保留组合（不可拦截）

系统 MUST 在规格与代码中维护一份保留组合清单，覆盖 **Chrome、Safari、Firefox、Edge** 在 **macOS 与 Windows/Linux** 下的常见快捷键。Web 应用 MUST NOT 尝试将下列组合作为可绑定动作（引擎不注册、设置页不可保存）：

| 类别 | 代表组合（⌘=Mac Command，Ctrl=Win/Linux） | 行为 |
|------|-------------------------------------------|------|
| 窗口/标签 | ⌘/Ctrl+W、⌘+Shift+W | 关闭标签/窗口 |
| 新建 | ⌘/Ctrl+T、⌘/Ctrl+N、⌘+Shift+N | 新标签/窗口/无痕 |
| 退出/最小化 | ⌘+Q、⌘+M | 退出/最小化（macOS） |
| 刷新 | ⌘/Ctrl+R、⌘+Shift+R、F5 | 刷新 |
| 地址栏 | ⌘/Ctrl+L、Alt+D | 聚焦地址栏 |
| 查找/打印/保存 | ⌘/Ctrl+F、⌘/Ctrl+P、⌘/Ctrl+S | 查找/打印/存页 |
| 编辑 | ⌘/Ctrl+A/C/V/X/Z | 全选/复制/粘贴/剪切/撤销 |
| 标签切换 | ⌘/Ctrl+1…9 | 切换标签 |
| 应用切换 | ⌘+Tab、Ctrl+Tab | OS/浏览器标签切换 |

#### Scenario: 不拦截关闭标签组合

- **WHEN** 用户按下 ⌘+W 或 Ctrl+W
- **THEN** 浏览器执行关闭标签，引擎不 preventDefault

### Requirement: 单字母禁用池（按作用域）

系统 MUST 通过 `src/plugins/shortcut/reserved-keys.js` 导出统一常量，至少包含：

| 常量 | 禁用字母 | 作用域 |
|------|----------|--------|
| `PAGE_ACTION_RESERVED` | A, C, M, N, Q, T, V, X, Z | 页面动作 `registerPage`、`page-action-hints`、行动态分配 |
| `FIELD_HINT_BLOCKED` | PAGE 集合 + N, T, W, Q, R, L, H | 表单字段 `form-field-hints` |
| `SETTINGS_NEVER_BIND` | 上述并集 | 键盘设置页用户自定义 |

字母 **A/C/V/X/Z** MUST 永不分配给任何字段或页面动作映射。

字母 **N/T/W/Q/R/L/H/M** MUST 按上表在对应作用域禁止分配。

#### Scenario: 页面动作不分配 T

- **WHEN** 为测试计划页分配默认动作字母
- **THEN** `T` 不在 `PAGE_ACTION_RESERVED` 允许池内，默认值不为 `T`

#### Scenario: 字段池不分配 L

- **WHEN** 用例抽屉内为可见字段分配 ⌘+字母
- **THEN** `L` 不会出现在字段徽标上

### Requirement: 单一数据源与迁移

`page-action-hints.js`、`form-field-hints.js`、`defect-row-kbd-hints.js`、`shortcut-store.js` MUST 从 `reserved-keys.js` import，禁止各文件维护独立、不一致的保留集合。

#### Scenario: 新增保留字母一处生效

- **WHEN** 维护者在 `reserved-keys.js` 将某字母加入 `SETTINGS_NEVER_BIND`
- **THEN** 键盘设置页与运行时分配同时拒绝该字母

### Requirement: 导航键与 W 的规避

全局导航「项目文档」默认字母 MUST 为 **F**（`g+F`），不得使用 **W**，以避免与 ⌘/Ctrl+W 关闭标签语义混淆（Phase 1 已落地，Phase 2 MUST 保持）。

#### Scenario: 导航文档不用 W

- **WHEN** 用户查看默认导航字母表
- **THEN** 文档管理导航键为 F 而非 W

### Requirement: 用户可见说明

键盘设置页 MUST 提供「不可绑定键」说明区（可折叠），摘要列出：系统编辑键、浏览器窗口/标签键、地址栏/刷新相关键，并链到用户指南（实现后）。

#### Scenario: 设置页展示保留键说明

- **WHEN** 用户打开键盘设置页
- **THEN** 可见不可绑定键说明，且尝试绑定保留字母时被拒绝并提示
