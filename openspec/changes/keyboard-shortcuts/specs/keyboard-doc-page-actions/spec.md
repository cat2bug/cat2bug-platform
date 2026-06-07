## ADDED Requirements

### Requirement: 系统文档页动作注册

系统使用手册页（`/system/doc`，`views/doc/index.vue`）SHALL 在 `activated` 时注册 `registerPage('system-doc', …)`，离开页面 MUST 注销。

页面 MUST 混入 `page-action-hints`：按住 `Cmd/Ctrl` 时在搜索框、返回、目录树、本页大纲、打印按钮等处显示浮层徽标；也可通过动作引导键 `空格` 打开命令面板后按字母触发。

#### Scenario: 离开页面注销

- **WHEN** 用户离开系统文档页
- **THEN** 注销 `system-doc` 动作，不再响应其字母键

### Requirement: 系统文档页默认动作键

系统 MUST 提供以下默认动作字母（用户可在键盘设置「系统文档页」分组中修改）：

| 键 | 动作 |
|---|---|
| S | 聚焦搜索框（并选中已有文本） |
| B | 返回上一页 |
| L | 进入左侧目录树键盘导航 |
| D | 进入右侧本页大纲键盘导航（大纲项 ≥ 2 时可用） |
| P | 打印当前文档（大纲项 ≥ 2 时可用） |

大纲导航与打印 MUST 在 `outlineItems.length < 2` 时不显示徽标、不响应快捷键。

#### Scenario: Cmd/Ctrl 聚焦搜索

- **WHEN** 用户在系统文档页按住 `Cmd/Ctrl` 后按 `S`，或按 `空格` 后按 `S`
- **THEN** 搜索框获得焦点且选中已有内容，并退出目录/大纲导航会话

#### Scenario: 进入目录树导航

- **WHEN** 用户按 `L`（浮层或命令面板）
- **THEN** 进入目录树键盘导航，高亮当前文档对应叶子项（若无则首项）

### Requirement: 搜索框与目录树方向键衔接

搜索框聚焦时按 `↓` MUST 进入目录树导航：失焦搜索框、从过滤后树的第一片叶子开始高亮。

目录树导航中：

| 按键 | 行为 |
|---|---|
| `↑` / `↓` | 在过滤后树的**叶子节点**列表间移动（`flattenDocTreeLeaves`） |
| 首项 `↑` | 退出目录导航并重新聚焦搜索框 |
| `Enter` | 打开当前高亮文档（等效点击树节点） |
| `Esc` | 退出目录导航（见 Esc 行为） |

树导航 MUST 通过 document 级 `keydown` 监听实现，**MUST NOT** 对侧栏容器调用 `focus()`，**MUST NOT** 显示容器级蓝色聚焦描边（仅保留 `el-tree` `is-current` 行背景高亮）。

重新聚焦搜索框时 MUST 退出目录树导航。

#### Scenario: 搜索框下键进入目录

- **WHEN** 用户在搜索框按 `↓` 且过滤结果含至少一片叶子
- **THEN** 搜索框失焦，目录树导航从首项开始

#### Scenario: 目录首项上键回搜索

- **WHEN** 用户在目录树导航首项按 `↑`
- **THEN** 退出目录导航并聚焦搜索框

### Requirement: 本页大纲方向键导航

进入大纲导航（`D`）后：

| 按键 | 行为 |
|---|---|
| `↑` / `↓` | 在大纲条目间移动 |
| `Enter` | 滚动至对应标题并退出大纲导航 |
| `Esc` | 退出大纲导航 |

大纲键盘焦点 MUST 通过 `DocPageOutline` 的 `is-kbd-focus` 样式显示（左边框 + 浅底），与滚动联动高亮的 `is-active` 可并存。

#### Scenario: 大纲 Enter 跳转标题

- **WHEN** 用户在大纲导航中按 `Enter`
- **THEN** 正文滚动至对应标题并退出大纲导航

### Requirement: Esc 行为

系统文档页 Esc MUST 分两级处理（document 捕获阶段，输入态除外）：

1. 若处于目录树或大纲导航（`docKbdRegion` 非空）：退出区域导航，不离开页面
2. 若未在区域导航、无遮挡层、焦点不在 input/textarea/select/contenteditable：返回上一页（等效页头返回）

#### Scenario: 导航中 Esc 退出导航

- **WHEN** 用户在目录树或大纲导航中按 `Esc`
- **THEN** 退出区域导航，仍停留在系统文档页

#### Scenario: 未导航时 Esc 返回

- **WHEN** 用户未在任何区域导航中、且焦点不在可编辑控件内按 `Esc`
- **THEN** 返回上一页

### Requirement: 键盘设置分组

键盘设置页 MUST 包含「系统文档页」独立分组，绑定 ID 为 `action.system-doc.query`、`action.system-doc.back`、`action.system-doc.treeNav`、`action.system-doc.outlineNav`、`action.system-doc.print`。

#### Scenario: 设置页显示系统文档分组

- **WHEN** 用户打开键盘设置页
- **THEN** 可见「系统文档页」分组及 S/B/L/D/P 五项可编辑快捷键
