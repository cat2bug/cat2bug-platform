## ADDED Requirements

### Requirement: 用例页动作面板

测试用例页（`/project/case`）SHALL 在激活时通过 `registerPage('case', …)` 注册动作，`activated/deactivated` 配对注销。动作引导键 MUST 仅在一级列表界面且无遮挡层时生效（见 `keyboard-shortcut-engine`）。

#### Scenario: 离开用例页注销

- **WHEN** 用户导航离开用例页
- **THEN** `unregisterPage('case')` 且空格不再展示用例动作

### Requirement: Cmd/Ctrl 工具栏徽标

用例页 SHALL 混入 `page-action-hints`，按住 `Cmd/Ctrl` 时在工具栏显示与动作面板一致的字母徽标。

#### Scenario: 按住 Cmd 显示 E 新建徽标

- **WHEN** 用户在一级用例列表按住 `Cmd/Ctrl` 且有新建权限
- **THEN** 「新建用例」按钮显示默认字母 E 徽标

### Requirement: 用例页默认动作字母

| 键 | 动作 |
|----|------|
| E | 新建用例（`handleAdd`） |
| U | 导入 |
| R | 导出 |
| I | AI 创建 |

字母 MUST 符合 `PAGE_ACTION_RESERVED` 约束；U/R/I/E 均允许。

#### Scenario: E 新建用例

- **WHEN** 用户按空格后 E，或按住 `Cmd/Ctrl` 后 E
- **THEN** 打开新建用例抽屉

#### Scenario: U 导入用例

- **WHEN** 用户按空格后 U，或按住 `Cmd/Ctrl` 后 U
- **THEN** 打开导入用例对话框

#### Scenario: R 导出用例

- **WHEN** 用户按空格后 R，或按住 `Cmd/Ctrl` 后 R
- **THEN** 执行导出用例

#### Scenario: I AI 创建用例

- **WHEN** 用户按空格后 I，或按住 `Cmd/Ctrl` 后 I
- **THEN** 打开 AI 用例生成

### Requirement: 表格行动态徽标

按住 `Cmd/Ctrl` 时，系统 SHOULD 为视口内可见行在编号列分配 1–9 动态字母；再按数字打开该行编辑（`handleUpdate`）。分配 MUST 与工具栏字母去重并遵守 `ROW_KBD_RESERVED`。

#### Scenario: Cmd+1 打开首行编辑

- **WHEN** 表格首行可见且分配数字 1
- **THEN** 按住 `Cmd/Ctrl` 再按 1 打开该行编辑抽屉

### Requirement: 查询区左右键导航

用例页查询条 MUST 支持 **← / →** 在「用例编号 → 标题 → 级别」间切换（见 `keyboard-shortcut-engine` 列表查询区导航）。该导航不通过页面动作快捷键触发。

#### Scenario: 查询区内左右切换

- **WHEN** 用户聚焦用例编号且处于查询导航态，按 **→**
- **THEN** 焦点移至标题输入框
