## 1. 数据与权限

- [x] 1.1 新增 Flyway `V1_1_0_45`：`sys_case_type`、`sys_case_review`、`sys_case_review_item`，`sys_case.review_status`，`sys_project.case_review_template`；语句可重复执行
- [x] 1.2 为已有项目补预置类型「功能」「接口」「界面」（同名跳过）；新建项目插入同样三项
- [x] 1.3 菜单权限：`system:project:case-type:list|add|edit|remove` 只绑项目创建人与项目管理员；`system:case:review` 绑项目创建人、项目管理员、项目测试
- [x] 1.4 `McpModules` 的 `project` 前缀追加 `system:project:case-type:`，不新增模块键

## 2. 类型与评审接口

- [x] 2.1 类型名单的查询、新增、改名、删除；删除时若仍被用例引用则拒绝并返回条数
- [x] 2.2 用例保存接受可空 `case_type`，非空时必须属于本项目；列表与详情返回类型 id 和名称
- [x] 2.3 发起评审、填写结论与意见、完成评审；一条用例不能同时处于两个进行中的评审；完成前每条结论必须是通过或需修改
- [x] 2.4 内置 Markdown 模版与项目覆盖模版；完成时生成纪要；完成后只改正文不回写章
- [x] 2.5 用例列表与计划选用例查询增加类型、评审筛选；保存计划不检查评审章
- [x] 2.6 删除用例时清理其进行中的评审子项；子项清空则删除该进行中评审

## 3. 界面

- [x] 3.1 项目设置中维护类型名单，以及评审纪要模版文本（空则用内置）
- [x] 3.2 用例表单增加可空类型；用例列表可按类型和评审筛选，勾选后可发起评审并编辑纪要
- [x] 3.3 `AddPlanDialog` 查询条增加类型与评审筛选项，默认不限
- [x] 3.4 新增文案写入 7 份 i18n；纪要 Markdown 渲染走现有清洗

## 4. MCP

- [x] 4.1 `list_case_types`、`create_case_type`、`update_case_type`、`delete_case_type`；后三者仅创建人/管理员，删除规则与网页一致
- [x] 4.2 `search_cases` 增加 `case_type_id`、`review_status`；`get_case` / `create_case` / `update_case` 读写类型并返回 `caseTypeName`、`reviewStatus`
- [x] 4.3 `start_case_review`、`comment_case_review`、`complete_case_review`、`update_case_review_report`、`list_case_reviews`、`get_case_review`；`create_case_type` 与 `start_case_review` 支持 `idempotency_key`
- [x] 4.4 更新 `mcp-guide/zh-CN/case.md`、`get_guide` 用例实况（列出类型名）、`McpGuideTools` 的评审操作、`write_test_cases` 引导

## 5. README 与首页版本功能

- [x] 5.1 更新根目录 `README.md`「内置功能」第 4 条和「最新版本更新说明」：项目可配置用例类型，可勾选评审并生成 Markdown 纪要，计划里评审只是筛选项；版本号保持 1.1.0
- [x] 5.2 在 `cat2bug-platform-ui/src/views/index.vue` 更新日志的 v1.1.0 条目追加同样说明

## 6. 验证

- [x] 6.1 后端测试覆盖：预置类型、删除被引用类型被拒绝、跨项目类型被拒绝、评审中冲突、完成纪要、计划保存未评审用例、MCP 工具与幂等
- [x] 6.2 浏览器核对用例列表筛选、发起并完成评审、计划新建/编辑筛选项默认不限且未评审用例可勾选；暗色下纪要正文可读

## 7. 回写长期知识

- [x] 7.1 更新 `docs/01-产品需求/产品定位与核心功能.md`、`docs/01-产品需求/术语表.md`、`docs/03-技术方案/MCP接入与权限.md`、`docs/05-接口文档/接口清单-缺陷与用例计划.md`、`docs/05-接口文档/接口清单-OpenAPI与AI.md`
- [x] 7.2 在 `docs/08-版本记录/2026-10.md` 增加本变更一行
- [x] 7.3 拟新增 ADR：无（不推翻 ADR-012 / ADR-014 / ADR-015 / ADR-017 / ADR-018）
- [x] 7.4 可复用踩坑：无。删除被引用类型、跨项目类型、进行中评审冲突、完成纪要、未评审用例加入计划、MCP 幂等已由 `QuarkusMcpServerTest.caseTypeAndReview` 覆盖
