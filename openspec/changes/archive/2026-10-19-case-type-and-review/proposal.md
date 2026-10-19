## Why

测试用例只有等级和交付物，无法按测试种类筛选，也没有可选的评审。小团队需要继续「写完就能进计划」；大团队需要项目内自管类型、对勾选的用例做一次评审并留下 Markdown 纪要。`sys_case.case_type` 已存在但界面与历史数据均为空，等于没交付。

## What Changes

- 每个项目有一份用例类型名单。新项目预置「功能」「接口」「界面」，项目创建人与项目管理员可增删改。仍被用例使用的类型拒绝删除。用例上的类型可空。
- 用例列表可勾选并发起一次评审（进行中 → 已完成）。完成时按内置 Markdown 模版生成纪要，评审人可改后再存。项目创建人与项目管理员可在项目设置里整段替换模版。用例上只保留最近一次结论（未评审 / 评审中 / 通过 / 需修改），不阻断执行。
- 测试计划新建与编辑的选用例条增加类型、评审两个筛选项，默认不限。未评审用例仍可勾进计划。不做门禁。
- MCP 在现有 `case`、`project` 模块上映射类型、评审与筛选，并更新 `case` 指南与 `write_test_cases` 引导。不新增 MCP 模块，不恢复已删除的报告模块（[[ADR-018-移除报告模块且不保留历史数据]]）。
- 不升版本号。把本能力写进根目录 `README.md` 的「内置功能」与「最新版本更新说明」，以及首页（`cat2bug-platform-ui/src/views/index.vue`）更新日志中现有的 v1.1.0 条目。

## Capabilities

### New Capabilities

- `case-type-catalog`: 项目级用例类型名单，以及用例上可空的类型与筛选。
- `case-review`: 勾选发起的可选评审、Markdown 纪要、计划选用例时的评审筛选（无门禁）。

### Modified Capabilities

- `mcp-tools`: 用例读写与查询带上类型和评审结论；新增类型名单与评审的精做工具。
- `mcp-usage-guide`: `case` 主题说明类型来自本项目名单、评审不是用例状态，实况列出当前类型。
- `mcp-guided-prompts`: `write_test_cases` 引导在创建前读取本项目类型，类型可空，不要求先评审。

## Impact

- 数据：新迁移（当前最新为 `V1_1_0_44`）。复用 `sys_case.case_type` 存类型 id；新增类型表、评审表、纪要正文与项目模版覆盖。
- 后端：用例、计划选例查询、项目设置接口；权限串 `system:project:case-type:*`（挂已有 MCP 模块 `project`）、`system:case:review`（挂已有模块 `case`）。
- 前端：项目设置、用例表单与列表、计划新建/编辑弹窗（`AddPlanDialog`）、评审与纪要；首页更新日志。
- MCP：`McpWorkTools`、`McpJson`、`mcp-guide/zh-CN/case.md`、`McpGuideTools`、`McpGuidePrompts`。
- 文档：`README.md`；首页更新日志；`docs/01-产品需求/`、`docs/03-技术方案/MCP接入与权限.md`、`docs/05-接口文档/`、`docs/08-版本记录/2026-10.md`。不新增 ADR（不推翻 ADR-014 / ADR-015 / ADR-017 / ADR-018）。
