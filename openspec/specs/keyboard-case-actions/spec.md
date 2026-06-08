## ADDED Requirements

### Requirement: 用例页动作面板

测试用例页（`/project/case`）SHALL 在激活时通过 `registerPage('case', …)` 注册动作，`activated/deactivated` 配对注销。动作引导键 MUST 仅在一级列表界面且无遮挡层时生效（见 `keyboard-shortcut-engine`）。

#### Scenario: 离开用例页注销

- **WHEN** 用户导航离开用例页
- **THEN** `unregisterPage('case')` 且空格不再展示用例动作

### Requirement: Cmd/Ctrl 工具栏徽标

用例页 SHALL 混入 `page-action-hints`，按住 `Cmd/Ctrl` 时在工具栏显示与动作面板一致的字母徽标。

#### Scenario: 按住 Cmd 显示 S 查询徽标

- **WHEN** 用户在一级用例列表按住 `Cmd/Ctrl`
- **THEN** 查询区显示默认字母 S 徽标

### Requirement: 用例页默认动作字母

| 键 | 动作 |
|----|------|
| S | 聚焦查询区 |
| E | 新建用例（`handleAdd`） |
| U | 导入 |
| R | 导出 |
| A | AI 创建 |
| D | 批量删除（已勾选时） |
| M | 切换模块树显隐 |
| G | 模块树键盘导航（树可见时） |
| B | 上一页 |
| P | 下一页 |

字母 MUST 符合 `PAGE_ACTION_RESERVED` 约束；U/R/A/D/M/G/B/P/S/E 均允许。

#### Scenario: E 新建用例

- **WHEN** 用户按空格后 E，或按住 `Cmd/Ctrl` 后 E
- **THEN** 打开新建用例抽屉

#### Scenario: M 切换模块树

- **WHEN** 用户按 M 且模块树当前可见
- **THEN** 隐藏模块树；再次按 M 显示

### Requirement: 模块树导航（G）

模块树可见时，系统 MUST 注册 G。进入导航后：

- `↑/↓`：在树节点间移动焦点
- `Enter`：选中当前模块（等效点击）
- `Esc`：退出树导航

树隐藏时 MUST NOT 注册或响应 G。

#### Scenario: 树隐藏时不响应 G

- **WHEN** 模块树已折叠隐藏
- **THEN** 不显示 G 徽标，按 G 无效果

### Requirement: 表格行动态徽标

按住 `Cmd/Ctrl` 时，系统 SHOULD 为视口内可见行在编号列分配 1–9 动态字母；再按数字打开该行编辑（`handleUpdate`）。分配 MUST 与工具栏字母去重并遵守 `ROW_KBD_RESERVED`。

#### Scenario: Cmd+1 打开首行编辑

- **WHEN** 表格首行可见且分配数字 1
- **THEN** 按住 `Cmd/Ctrl` 再按 1 打开该行编辑抽屉

### Requirement: 查询区左右键导航

用例页查询条 MUST 支持 **← / →** 在「用例编号 → 标题 → 级别」间切换（见 `keyboard-shortcut-engine` 列表查询区导航）。按 **S** 进入查询导航并聚焦首项。

#### Scenario: 查询区内左右切换

- **WHEN** 用户聚焦用例编号且处于查询导航态，按 **→**
- **THEN** 焦点移至标题输入框
