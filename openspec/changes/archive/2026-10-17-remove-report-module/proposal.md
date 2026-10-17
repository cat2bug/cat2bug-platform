## Why

项目里的报告模块长期没有使用，继续留着会占菜单、权限、接口、模版编辑器和三张业务表。产品以后不再提供报告。本次同时推翻 [[ADR-009-报告模版编辑器用Milkdown且DSL为真相源]]：报告模版不再存在，Milkdown 与 Cat2Bug DSL 不再作为产品能力保留。

## What Changes

- **BREAKING** 去掉项目内报告：列表、详情、批注、编辑历史、Word/PDF 导出、报告模版（含 Milkdown 编辑器与国标预制模版）。
- **BREAKING** 去掉 `POST /api/report/defect`。该接口在写入报告的同时创建或更新缺陷；删除后，Cat2Bug-JUnit / JLog 不能再通过这个地址推送。缺陷仍可通过既有缺陷接口进入。
- **BREAKING** 删除表 `sys_report`、`sys_report_template`、`sys_report_history`，以及报告批注（`module_type` 为 `report`、`report_history` 的评论）。已有报告正文、模版、批注和历史不导出、不保留。
- 去掉菜单与权限 `system:report:*`，仪表盘报告统计，新建报告的站内通知与钉钉 / 飞书 / 企业微信推送，MCP 的 `list_reports`、`get_report` 与指南中的报告主题。
- 去掉计划表上的 `sys_plan.report_id`。计划、计划项、缺陷、用例、文档保持不变。
- 登录页宣传文案、MCP prompt `report_defect`（提交缺陷）不在本次范围。

## Capabilities

### New Capabilities

- 无。

### Modified Capabilities

- `report-template-dsl`、`report-template-milkdown-editor`、`report-template-preset-groups`、`report-template-variable-preview`、`report-annotation`、`report-detail-layout`、`national-standard-default-templates`、`cat2bug-report-dsl-skills`：整项能力删除。
- `project-scoped-module-routes`：项目模块白名单去掉 `report`。
- `mcp-tools`：精做工具不再包含报告的列出与读取。
- `mcp-usage-guide`：`get_guide` 不再提供 `report` 主题。
- `im-external-push`：不再在报告新建时发通知。
- `single-backend-layout`：不再要求同步报告预制模版脚本。
- `html-render-sanitization`：清洗规则仍适用于其余用户内容；不再要求保留报告排版。

## Impact

- 前端：`views/system/report/`、`components/Report/`、`components/ReportTemplateEditor/`、报告模版选择器、项目路由与快捷键、各语言 `report.*`、`@milkdown/*` 依赖。Git 仓库浏览仍要 Markdown 标题提取，实现须从报告目录挪出后再删目录。
- 后端：`ReportResource`、`ReportTemplateResource`、`OpenApiReportResource` 及对应 service / mapper；仪表盘 `/{projectId}/report`；项目云备份中的报告 JSON；classpath `报告/模版/`；个人通知配置里的报告事件。
- 数据：新 Flyway 脚本删除三张报告表、报告评论和 `sys_plan.report_id`，并删除菜单与角色菜单关联。不改已经发布的 `V0_5_1__init.sql`。
- 文档：ADR-009 标为被新 ADR 取代；`docs/01`～`05` 中把报告写成现有功能的段落在归档时回写。`test-standards-skills` 仍描述标准里的测试文档种类，不随本模块删除。
