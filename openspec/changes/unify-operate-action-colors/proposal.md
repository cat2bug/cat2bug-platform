## Why

业务列表操作列语义色不统一：缺陷列表已约定「修改=绿、删除=红」（`green` / `red` + `--cat2bug-operate-text-*`），但交付物、OpenAI 账号、用例类型等页的「修改」未打 `green`，与「添加」同为默认蓝；「添加」也缺少显式蓝色语义 class。用户扫一眼操作列时无法靠颜色区分增改删。

## What Changes

- 约定列表操作列 CRUD 三色：**修改 → 绿**（`green` / success token）、**添加 → 蓝**（显式 `blue` + primary token）、**删除 → 红**（`red` / danger token）；颜色一律走全局 `--cat2bug-operate-text-*`（及新增的 primary 映射），不写死色值。
- 补齐 `.blue`（或等价）样式，与现有 `.green` / `.red` / `.orange` 并列；浅色与暗色均覆盖。
- 扫业务列表操作列：修改补 `green`，行内添加补 `blue`，删除保持/补 `red`（首批含交付物、OpenAI 账号、用例类型及同模式页；缺陷列表修改/删除已对齐，仅确认）。
- 更新 `docs/04-UI规范/组件与交互约定.md` 操作列语义色约定。
- **不改**：缺陷流转钮的 `orange`（指派/修复等）、工具栏「新建」按钮样式、对话框确认钮、后端接口。

## Capabilities

### New Capabilities

- `operate-action-colors`: 列表操作列增改删（及同类 text/link）的语义色 class 与 token 约定。

### Modified Capabilities

- （无）`openspec/specs/` 中无既有操作色级需求规格需改写。

## Impact

- 前端：`cat2bug.scss` / `theme-dark.scss`（`.blue` + token）；各业务列表页操作列 `el-button` class；`DefectTools` 仅核对、原则上不改流转色。
- 文档：`docs/04-UI规范/组件与交互约定.md`；版本记录一行；拟新增 ADR：无。
- 无 API / 数据库 / Native 体积变更。
