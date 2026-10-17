# 报告模版 DSL 与 Milkdown 编辑器（已退役）

本能力已随产品下线报告模块而移除。决策见 [[ADR-018-移除报告模块且不保留历史数据]]（取代 [[ADR-009-报告模版编辑器用Milkdown且DSL为真相源]]）。

- 表 `sys_report`、`sys_report_template`、`sys_report_history` 由迁移 `V1_1_0_43` 删除。
- 前端不再包含 `ReportTemplateEditor`、Milkdown 或报告详情；缺陷等页面的 Markdown 仍用 `Cat2BugMarkdown`。
- 历史规格目录名仍可能出现在 `openspec/specs/` / 归档变更中，仅作考古，不再作为产品要求。

返回 [[00-项目总览]]
