## Why

报告模版编辑目前基于 **CodeMirror 源码编辑 + markdown-it 预览**（`Cat2BugMarkdown`），用户需手写 Cat2Bug DSL（`${api.*}`、`$table{}`、`$card{}` 等），学习成本高、易出错，且无法直观配置缺陷列表等动态块。在 Vue 3 + Vite 迁移完成后，引入 [Milkdown](https://milkdown.dev) 作为 WYSIWYG 编辑层，可在保留既有 DSL 存储格式与导出链路的前提下，显著降低模版编辑门槛。

## What Changes

- 新增 Milkdown 驱动的报告模版编辑器组件，逐步替换 `EditReportTemplate.vue` 中的 `Cat2BugMarkdown` 编辑区
- 为高频 DSL 构造提供可视化块节点（chip / block）：项目变量、计划变量、缺陷表格、用例卡片、动态图片
- 实现 DSL **parse → Milkdown doc → serialize** 双向转换，保证 `sys_report_template.template_content` 存储格式不变
- **保留**现有 `markdown-it` 预览/渲染链路（`ViewReport.vue`、Word/PDF 导出）——编辑层替换，渲染层不动
- **保留**工具栏语义（`MarkdownTools.js` 能力映射为 Milkdown 插入命令），含计划统计大表一键插入
- Phase 0 以缺陷表格块为 spike 验收；P1 扩展用例卡片与计划变量；P2 处理计划统计大表与 multimd 合并表（只读或源码块）
- 不修改后端 API（`/markdown/{projectId}`、`sys_report_template` 字段结构）
- 不 **BREAKING**：既有模版无需迁移即可继续渲染；新编辑器须能打开并回写旧模版

## Capabilities

### New Capabilities

- `report-template-dsl`: Cat2Bug 报告模版 DSL 语法契约、parse/serialize 规则、与 markdown-it 渲染器的兼容边界
- `report-template-milkdown-editor`: Milkdown 集成——自定义节点/视图、工具栏、编辑预览分栏、与 `EditReportTemplate` 的 v-model 对接

### Modified Capabilities

（无——`openspec/specs/` 尚无报告模版相关 capability spec）

## Impact

- **代码范围**：`cat2bug-platform-ui/src/components/Cat2BugMarkdown/`（逐步废弃编辑职责）、新建 `src/components/ReportTemplateEditor/`；`EditReportTemplate.vue` 接入新组件；`ViewReport.vue` 与 `plugins/*.js` 仅作回归验证，原则上不改动
- **依赖**：新增 `@milkdown/*` 系列（core、preset、plugin 等）；不移除 `markdown-it` / `md-editor-v3` 直至迁移完成
- **数据**：`template_content` 仍为纯文本 Markdown + DSL；项目导出 `report_template.json` 格式不变
- **测试**：新增 unit 测试（DSL round-trip）；扩展 Playwright 覆盖模版编辑页插入缺陷表、保存、预览一致性
- **风险**：计划统计超宽表、multimd-table 合并单元格、边界 DSL（列别名、多计划索引）的 round-trip  fidelity
