## ADDED Requirements

### Requirement: 报告列表动作面板

项目报告页 SHALL `registerPage('report', …)` + `page-action-hints`。

### Requirement: 报告列表默认字母

| 键 | 动作 |
|----|------|
| S | 聚焦标题查询 |
| E | 新建报告（打开模板选择浮层） |
| D | 批量删除（已勾选） |
| B | 上一页 |
| P | 下一页 |

> **N** 在 `PAGE_ACTION_RESERVED` 内（⌘/Ctrl+N 新窗口），故新建报告使用 **E**，与其它列表页一致。

#### Scenario: E 打开模板选择浮层

- **WHEN** 用户有 `system:report:add` 权限且按 E
- **THEN** 展开「生成报告」模板选择浮层并聚焦首张模板卡片

#### Scenario: 浮层打开默认聚焦首张卡片

- **WHEN** 模板选择浮层展开（鼠标悬停、E、或触发按钮上 Enter/Space/↓）
- **THEN** 首张模板卡片显示键盘焦点框并获焦

#### Scenario: 浮层内方向键选择模板

- **WHEN** 模板浮层已打开且用户按 **↓** 或 **→**
- **THEN** 焦点在模板卡片网格内移动并显示焦点框

#### Scenario: 首行按 ↑ 关闭浮层

- **WHEN** 焦点位于首行模板卡片且用户按 **↑**
- **THEN** 浮层关闭，焦点回到「生成报告」按钮

#### Scenario: Space/Enter 选择模板

- **WHEN** 某模板卡片处于键盘焦点且用户按 Space 或 Enter
- **THEN** 以该模板创建报告并关闭浮层

#### Scenario: ⌘/Ctrl+A 添加模版

- **WHEN** 模板浮层已打开且用户按住 **⌘/Ctrl** 并按 **A**
- **THEN** 执行「添加模版」与点击链接相同；按住 **⌘/Ctrl** 时在链接上显示 **A** 徽标

#### Scenario: ⌘/Ctrl+B/P 模板分页

- **WHEN** 模板浮层已打开、存在多页模板且用户按住 **⌘/Ctrl** 并按 **B** 或 **P**
- **THEN** 分别切换到上一页或下一页，并聚焦首张模板卡片；按住 **⌘/Ctrl** 时在分页按钮上显示 **B** / **P** 徽标

#### Scenario: 聚焦卡片时 ⌘/Ctrl+V/E/D 复制、编辑与删除

- **WHEN** 模板浮层已打开、某模板卡片处于键盘焦点且用户按住 **⌘/Ctrl** 并按 **V**、**E** 或 **D**
- **THEN** 分别执行复制、编辑或删除该模板；按住 **⌘/Ctrl** 时在卡片右上角对应按钮显示 **V** / **E** / **D** 徽标

### Requirement: ViewReport

查看报告组件打开时 MUST 视为遮挡层；一级报告动作禁用。ViewReport 内 `Esc` MUST 关闭查看层。

#### Scenario: 查看报告时禁用列表动作

- **WHEN** ViewReport 可见
- **THEN** 空格不打开报告列表动作面板

### Requirement: 查询区左右键导航

报告列表查询条 MUST 支持 **← / →** 在「标题 → 时间范围 → 推送人」间切换。日期或成员下拉展开时 MUST NOT 切换查询项。

#### Scenario: 查询区内左右切换

- **WHEN** 用户聚焦报告标题且处于查询导航态，按 **→**
- **THEN** 焦点移至时间范围控件

### Requirement: 报告列表 ⌘/Ctrl 方向键滚动

报告列表页在按住 **⌘/Ctrl** 且未打开查看抽屉、编辑弹框或「显示字段」浮层时，MUST 支持：

| 按键 | 动作 |
|------|------|
| ↑ / ↓ | 垂直滚动表格列表 |
| ← / → | 水平滚动表格列表 |

#### Scenario: ⌘/Ctrl + ↓ 垂直滚动

- **WHEN** 用户按住 **⌘/Ctrl** 且在报告列表一级页，按 **↓**
- **THEN** 表格垂直滚动条向下移动

#### Scenario: ⌘/Ctrl + → 水平滚动

- **WHEN** 用户按住 **⌘/Ctrl** 且表格列宽超出可视区，按 **→**
- **THEN** 表格水平滚动条向右移动

#### Scenario: 查看报告时不滚动列表

- **WHEN** ViewReport 抽屉可见
- **THEN** **⌘/Ctrl + 方向键** 不滚动背后列表

### Requirement: 报告模版编辑页 Esc 返回

报告模版编辑页（`/report/template/add`）MUST 支持 **Esc** 与标题栏返回按钮返回上一页。若标题或内容相对上次成功自动保存有未同步修改（含 debounce 自动保存尚未完成），MUST 先立即保存再离开，MUST NOT 弹出未保存确认对话框。

#### Scenario: 无未保存修改时 Esc 返回

- **WHEN** 用户在模版编辑页且内容与上次成功保存一致，按 **Esc**
- **THEN** 直接返回上一页

#### Scenario: 有未保存修改时 Esc 自动保存后返回

- **WHEN** 用户修改模版标题或内容且变更尚未成功保存，按 **Esc**
- **THEN** 立即触发保存；保存成功后返回上一页，不弹出确认

#### Scenario: 保存失败时不离开

- **WHEN** 用户按 **Esc** 且立即保存失败
- **THEN** 留在编辑页并展示错误提示
