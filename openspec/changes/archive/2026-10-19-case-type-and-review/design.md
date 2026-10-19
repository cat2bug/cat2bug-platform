## Context

用例是给人执行的：标题、交付物、等级、前置条件、步骤、预期。`sys_case.case_type`（可空整数）和 `SysCase.caseType` 已在，表单把该字段注释掉，保存恒为 `null`，历史数据为空。用例没有状态机；执行结果在测试计划子项上。

计划新建与编辑共用 `cat2bug-platform-ui/src/components/Plan/AddPlanDialog.vue`。选用例条已有编号、标题、等级和交付物树，勾选结果写入计划，查询条件不限制能否保存。

用例列表已有多选（`views/system/case/index.vue`）。MCP 精做工具在 `McpWorkTools`：`search_cases`、`get_case`、`create_case`、`update_case`。`McpJson.caseSummary` 会抄出 `caseType`，但值始终为空且没有名称。权限是角色权限与项目 MCP 模块的交集（`docs/03-技术方案/MCP接入与权限.md`，[[ADR-014-MCP权限为角色权限与项目模块的交集]]、[[ADR-015-MCP用精做工具加通用通道-经回环转发复用网页接口]]）。`project` 模块已覆盖 `system:project:defect-field:`；`case` 模块覆盖 `system:case:`。后加的模块键对已有项目默认不勾选（`McpModules`）。

报告模块已删除，不得恢复（[[ADR-018-移除报告模块且不保留历史数据]]）。缺陷属性配置的权限菜单挂在项目设置上，只发给项目创建人与项目管理员，类型名单沿用这一模式。

## Goals / Non-Goals

**Goals:**

- 项目内可增删改的用例类型；新项目与已有项目都预置「功能」「接口」「界面」。
- 用例类型可空；列表与计划选用例可按类型筛选。
- 勾选一批用例发起一次评审，完成时生成可编辑的 Markdown 纪要。
- 计划新建/编辑只把评审结论当筛选项，默认不限，不拒绝未评审用例。
- 网页上的字段与动作在 MCP 有同规则的精做工具，并更新指南与 `write_test_cases`。
- 根目录 `README.md` 与首页更新日志 v1.1.0 写上本能力。不升版本号。

**Non-Goals:**

- 门禁：未评审不得进计划。
- 按类型切换用例表单，或把「单元测试」做成代码执行器。
- 每条用例一条审批流。
- 恢复报告模块、Milkdown 模版 DSL、纪要版本历史。
- 新增 MCP 模块键。
- 登录页三张功能卡片（`login.features.*`）文案。

## Decisions

### 1. 类型是项目字典，用例只存 id

新表 `sys_case_type`：`type_id`、`project_id`、`type_name`、`sort_order`。同一项目内名称唯一。`sys_case.case_type` 继续存类型 id，`null` 表示未分类。

新建项目时插入三项预置。迁移对已有项目同样插入这三项（名称已存在则跳过），避免升级后下拉为空。

删除前统计引用条数，大于 0 则拒绝并返回条数。改名只改字典，用例不用迁移。

备选：全局枚举。小团队改不了名单，否决。备选：停用而不是删除。第一版只有「仍被使用则拒绝删除」，少一个状态。

### 2. 评审是一次活动，章记在用例上

`sys_case_review`：项目、状态（`open` / `completed`）、纪要 Markdown、创建人、完成时间。

`sys_case_review_item`：评审 id、用例 id、结论（`pending` / `passed` / `changes`）、意见。

`sys_case.review_status` 用字符串，可空：`null` 未评审，`in_review` 评审中，`passed` 通过，`changes` 需修改。用字符串，避免再走整数枚举把 JSON 契约做成序号（见 `docs/06-踩坑记录/实体枚举改成Integer后接口JSON契约悄悄变了.md`）。

一条用例同时只属于一个 `open` 评审。页面上点「发起评审」只打开弹框，不写库；勾选里已在评审中的用例被跳过并提示编号，其余照常进入，不因它们整批失败。弹框里结论是单选，并可以一次把全部设为通过或需修改。点「完成评审」才生成已完成评审和纪要，并把章写成各条结论。MCP 的 `start_case_review` 仍会先开一次进行中的评审，其中任一用例已在别的进行中评审里时整批失败；完成时才把章写成结论。

删除用例时删掉其进行中的评审子项；子项清空则删除该进行中评审。已完成评审的纪要是当时生成的 Markdown，不随用例删除而改写。

谁可以发起、写意见、完成：项目创建人、项目管理员、项目测试（新权限 `system:case:review`）。能查看用例的成员可以读评审和纪要。

### 3. 纪要是一份 Markdown，不是报告产品

内置模版放资源文件，完成时由服务端替换项目名、日期、评审人、用例编号、标题、类型名、结论和意见，写入 `sys_case_review` 的正文。完成后允许只改正文，不改结论，也不回写用例上的章。

项目设置提供一个多行文本，存 `sys_project.case_review_template`（可空）。空则用内置模版（`case-review/default-template.md`，栏目参考 GB/T 9386 与同行评审；完成时 `{{cases}}` 渲染为 Markdown 表格）。新建项目在 `seedDefaults` 时若模板为空则写入内置正文；GET 模板接口在库空时也返回内置，便于设置页直接看到。只有项目创建人与项目管理员可改，权限与类型名单相同，不单独做模版产品。长期说明见 `docs/03-技术方案/用例评审纪要模板.md`。

渲染沿用现有 Markdown 组件，遵守 [[ADR-012-用户内容在前端渲染时清洗而非后端转义]]。

### 4. 计划筛选不拦截

`AddPlanDialog` 的查询条增加类型（本项目名单，可清空）和评审（全部 / 未评审 / 评审中 / 通过 / 需修改）。默认全部。保存计划时不检查评审状态或类型。用例列表使用同一组查询参数，方便不进计划也能筛。

### 5. MCP 挂在已有模块上

不新增模块键，这样已勾选「测试用例」「项目」的项目不用再勾一次。

| 工具 | 权限 | 模块 |
|---|---|---|
| `list_case_types` | `system:case:list` | `case` |
| `create_case_type` / `update_case_type` / `delete_case_type` | `system:project:case-type:add` / `edit` / `remove` | `project` |
| `search_cases` 增加 `case_type_id`、`review_status` | 原权限 | `case` |
| `get_case` / `create_case` / `update_case` 读写 `case_type_id`，返回 `caseTypeName`、`reviewStatus` | 原权限 | `case` |
| `start_case_review` / `comment_case_review` / `complete_case_review` / `update_case_review_report` | `system:case:review` | `case` |
| `list_case_reviews` / `get_case_review` | `system:case:query` | `case` |

`create_case_type` 与 `start_case_review` 接受可选 `idempotency_key`，复用 `sys_mcp_idempotency`。类型 id 必须属于当前项目，否则与网页一样拒绝。`McpModules` 的 `project` 前缀追加 `system:project:case-type:`。

`get_guide(topic=case)` 的实况列出本项目类型名称。`mcp-guide/zh-CN/case.md` 写明类型可空、评审不是执行状态、计划筛选不拦截。`McpGuideTools.OPERATIONS` 的 case 增加「评审」。`write_test_cases` 要求先 `list_case_types`，类型可省略，不要求评审。

网页接口仍是真相源，精做工具经回环网关调用，与 [[ADR-015-MCP用精做工具加通用通道-经回环转发复用网页接口]] 一致。

### 6. 版本说明写在现有 1.1.0

不改 Maven 版本号。`README.md`「内置功能」第 4 条补上类型与可选评审；「最新版本更新说明」增加对应条目。首页 `views/index.vue` 更新日志里已有的 `v1.1.0 - 2026-07-12` 折叠项追加同样的中文条目（该列表是写死的中文，与现有条目一致）。

## Risks / Trade-offs

- [已有项目被插入三项预置，和用户自建重名] → 按项目 + 名称跳过；用户仍可删除未被引用的预置项。
- [用例同时进两次评审，章互相覆盖] → 进行中唯一；完成才写最终章。
- [纪要模版被改坏，完成时缺段落] → 模版只做字符串替换，缺占位符不失败；项目模版可清空以回到内置。
- [导入导出多出空列或旧文件无类型列] → 类型与评审状态在导入中可缺省，缺省保持空，不因缺列失败。
- [开放接口 `/api/case` 随 `SysCase` 带上新字段] → 写入时同样校验类型属于该项目；不另建契约。旧客户端忽略未知字段即可。
- [首页日志与 README 以后升版时条目留在 1.1.0] → 本次明确不升版；升版时再挪到新版本标题，不在本变更做。

## Migration Plan

1. 新增 Flyway `V1_1_0_45`（H2 / MySQL 共用 `db/migration`，语句可重复执行）：类型表、评审表、评审子项、`sys_case.review_status`、`sys_project.case_review_template`；为每个已有项目补三项预置；菜单权限 `system:project:case-type:*` 只绑角色 6 与 11，`system:case:review` 绑角色 6、11、8。
2. 先上接口与 MCP，再上界面。旧前端不传新字段时行为与现在相同。
3. 回滚：删新表、新列、新菜单。`case_type` 列保留（本来就在）。已写入的类型 id 在回滚后变成无字典的孤儿数字，回滚须同时恢复升级前备份，与 [[ADR-018-移除报告模块且不保留历史数据]] 的回滚说明同一级别。

## Open Questions

无。讨论已定：评审对象是勾选的一批用例；不做门禁；类型按项目配置，权限给项目创建人与项目管理员。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/01-产品需求/产品定位与核心功能.md`
- `docs/01-产品需求/术语表.md`
- `docs/03-技术方案/MCP接入与权限.md`
- `docs/03-技术方案/权限模型与鉴权链路.md`
- `docs/04-UI规范/组件与交互约定.md`（`Cat2BugEditor` / 错误整页约定）
- `docs/03-技术方案/用例评审纪要模板.md`
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`
- `docs/05-接口文档/接口清单-OpenAPI与AI.md`
- `docs/07-技术决策/ADR-012-用户内容在前端渲染时清洗而非后端转义.md`
- `docs/07-技术决策/ADR-014-MCP权限为角色权限与项目模块的交集.md`
- `docs/07-技术决策/ADR-015-MCP用精做工具加通用通道-经回环转发复用网页接口.md`
- `docs/07-技术决策/ADR-017-MCP使用指南以工具加Prompt交付-正文放资源文件按项目实况拼接.md`
- `docs/07-技术决策/ADR-018-移除报告模块且不保留历史数据.md`
- `docs/07-技术决策/ADR-019-可视化Markdown统一用Tiptap连续A4纸面.md`（实施中沉淀；本变更提案时拟新增 ADR：无）
- `docs/08-版本记录/2026-10.md`
