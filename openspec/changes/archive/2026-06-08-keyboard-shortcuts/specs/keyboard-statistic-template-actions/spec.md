## ADDED Requirements

### Requirement: 统计模版页动作注册

统计模版页（`StatisticTemplate.vue`）SHALL 在 `mounted/activated` 时注册 `registerPage('statistic-template', …)`，离开页面 MUST 注销。

页面 MUST 混入 `page-action-hints`：按住 `Cmd/Ctrl` 时在三块区域右下角显示浮层徽标；也可通过动作引导键 `空格` 打开命令面板后按字母进入。

#### Scenario: 离开页面注销

- **WHEN** 用户离开统计模版页
- **THEN** 注销 `statistic-template` 动作，不再响应其字母键

### Requirement: 统计模版页默认入口键

系统 MUST 提供以下默认动作字母（用户可在键盘设置「统计模版页」分组中修改）：

| 键 | 区域 |
|---|---|
| P | 预览区 |
| G | 个人模版 |
| H | 团队模版 |

#### Scenario: 进入个人模版导航

- **WHEN** 用户在统计模版页按住 `Cmd/Ctrl` 后按 `G`，或按 `空格` 后按 `G`
- **THEN** 进入个人模版区域键盘导航并聚焦第一项

### Requirement: 区域内方向键导航

进入某一区域导航后：

| 区域 | 方向键 | 确认 / 删除 |
|---|---|---|
| 预览区 | 仅 `←` / `→` | `Delete` / `Backspace` 从预览移除当前项 |
| 个人模版 / 团队模版 | `↑` `↓` `←` `→` 网格导航 | 空格 / `Enter` 将当前模版添加到预览区 |

网格导航实现 MUST 使用 `statistic-grid-kbd.js`，按视觉位置寻找相邻模块。

#### Scenario: 预览区左右移动

- **WHEN** 用户在预览区导航中按 `→`
- **THEN** 焦点移至右侧下一个预览模块

#### Scenario: 添加模版到预览

- **WHEN** 用户在个人模版导航中聚焦某模版并按空格
- **THEN** 将该模版添加到预览区

### Requirement: 跨区域上下焦点传递

当在当前区域内 `↑`/`↓` 无法继续移动且处于边界行时，系统 MUST 按水平中心 x 对齐将焦点传递到相邻区域：

- 团队模版**首行** `↑` → 个人模版**末行**最接近 x 的模块
- 个人模版**首行** `↑` → 预览区最接近 x 的模块
- 预览区 `↓` → 个人模版**首行**最接近 x 的模块
- 个人模版**末行** `↓` → 团队模版**首行**最接近 x 的模块

目标区域无模块时 MUST 保持当前焦点不变。

#### Scenario: 团队首行向上跨区

- **WHEN** 用户在团队模版首行按 `↑`
- **THEN** 焦点移至个人模版末行水平位置最接近的模块

#### Scenario: 目标区为空不跨区

- **WHEN** 个人模版区域没有任何模版且用户在预览区按 `↓`
- **THEN** 焦点保持在预览区

### Requirement: Esc 行为

统计模版页 Esc MUST 分两级处理：

1. 若处于区域导航（`tplKbdZone` 非空）：退出区域导航，不离开页面
2. 若未在区域导航：返回上一页（`$router.back()`）

不使用 `⌘+B` 作为返回快捷键。

#### Scenario: 导航中 Esc 退出导航

- **WHEN** 用户在个人模版导航中按 `Esc`
- **THEN** 退出区域导航，仍停留在统计模版页

#### Scenario: 未导航时 Esc 返回

- **WHEN** 用户未在任何区域导航中按 `Esc`
- **THEN** 返回缺陷列表上一页

### Requirement: 键盘设置分组

键盘设置页 MUST 包含「统计模版页」独立分组，绑定 ID 为 `action.statistic-template.preview`、`action.statistic-template.personal`、`action.statistic-template.team`。

#### Scenario: 设置页显示统计模版分组

- **WHEN** 用户打开键盘设置页
- **THEN** 可见「统计模版页」分组及 P/G/H 三项可编辑快捷键
