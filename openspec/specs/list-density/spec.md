# list-density Specification

## Purpose
TBD - created by archiving change unify-list-density. Update Purpose after archive.
## Requirements
### Requirement: 业务列表使用统一密度双轨

系统 SHALL 对业务列表页同时遵守控件轨与表格轨：工具栏/筛选控件高度为 32px（`--cat2bug-control-height`）；数据表单行最小行高约为 **56px**（`--cat2bug-table-row-min-height`），cell 上下留白约为 **16px**（`--cat2bug-table-cell-padding-y`），使单行相对 Element Plus default（约 8px / ~39px）肉眼可感更松。数据行最高高度 MUST 仍遵守现有上限（100px）。本要求 MUST NOT 把全站控件轨抬到超过 32px。

#### Scenario: 工具栏与表格行对比

- **WHEN** 用户打开任意业务列表页（含尚未迁到 Cat2BugTable 的页）并查看工具栏输入框与表格单行文本行
- **THEN** 工具栏控件高度仍为 32px，且单行数据行的可见高度明显高于改前偏扁状态（约 39px），且不低于约定的表格轨最小行高约 56px

#### Scenario: 多行单元格仍受最大行高约束

- **WHEN** 某单元格文本超过单行并在行内折行或截断
- **THEN** 该数据行高度不超过现有最大行高约定（100px）

### Requirement: 列表工具栏主操作视觉与筛选同轨

列表页工具栏上的主要新建/添加类按钮 SHALL 使用 `type="primary"` 且 `plain`，高度与同排筛选控件同为 32px，MUST NOT 在同一工具栏行使用无 plain 的实心 primary 作为唯一主操作以致相对输入框视觉明显「更大一号」。在暗色主题下，该类主按钮 MUST 呈现描边/轻底（例如 `cat2bug-btn-outline` 或 `.list-page-toolbar` 作用域样式），MUST NOT 被画成与实心 primary 相同的蓝底白字。对话框确认、登录等非列表工具栏场景不受本条约束。

#### Scenario: OpenAI 账号工具栏（浅色与暗色）

- **WHEN** 用户在浅色或暗色主题下打开 OpenAI 账号管理列表
- **THEN** 「新建」类主按钮为 primary plain，与左侧筛选输入框同高，且不以实心蓝块压过输入框观感（暗色下为描边/轻底）

#### Scenario: 交付物工具栏

- **WHEN** 用户打开交付物列表
- **THEN** 「新建交付物」为主色 plain（暗色下为描边/轻底），与「展开/折叠」及名称筛选同处工具栏轨

### Requirement: 操作列跟随表格轨

业务表操作列中的 link/text 操作 SHALL 在单元格内垂直居中，行内间距遵循现有操作列 gap token；MUST NOT 因缺少高度强制而在松后的行高中顶贴上沿或呈现比邻列更扁的「一条字」。

#### Scenario: 行内修改删除可读

- **WHEN** 用户查看交付物或账号列表某一数据行的操作列
- **THEN** 「修改」「删除」等 link 操作在行高内垂直居中，与名称列单行文本对齐舒适

### Requirement: 首批业务列表迁到 Cat2BugTable 与标准工具条

交付物列表与项目 OpenAI 账号列表 SHALL 使用 `Cat2BugTable`（或经批准的等价封装）展示数据，并使用 `.list-page-toolbar`（或文档约定的等价标准工具条 class）组织筛选与操作。交付物 MUST 保留树形展开/折叠与按名称筛选等既有能力。其他仍使用裸 `el-table` 的页面 MAY 仅先享受全局表格轨样式，完整迁移不作为本变更完成条件。

#### Scenario: 账号列表结构

- **WHEN** 维护者检查 OpenAI 账号列表页实现
- **THEN** 页面数据表为 Cat2BugTable（或项目约定封装），工具条为标准列表工具条结构

#### Scenario: 交付物树仍可用

- **WHEN** 用户在交付物列表点击展开/折叠并筛选名称
- **THEN** 树节点展开状态与筛选行为与改前一致可用

#### Scenario: 全局未迁页也变松

- **WHEN** 用户打开一个尚未迁到 Cat2BugTable 的业务列表页
- **THEN** 该页表格单行仍呈现表格轨的更松行高（由全局样式提供）

