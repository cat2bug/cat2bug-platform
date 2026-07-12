# 报告模版 DSL ↔ Milkdown 节点映射

本表描述 Cat2Bug 报告模版 DSL 与 `ReportTemplateEditor`（Milkdown）自定义节点的对应关系。持久化格式始终为 **纯文本 Markdown + DSL**（`template_content`），与 `ViewReport` / Word / PDF 导出共用同一套 `markdown-it` 插件链。

## 节点映射

| DSL 语法 | Milkdown 节点 | 编辑态行为 | 序列化 |
|----------|---------------|------------|--------|
| `${api.project.*}`、`${api.case.total}` | `cat2bug_variable` | 变量芯片 | 原文 `${...}` |
| `${api.plan.*[n]}` | `cat2bug_plan_variable` | 计划变量芯片（含索引） | 原文 `${...}` |
| `$table{api.defect.list[...]}` | `cat2bug_defect_table` | 缺陷表块 + 列配置 + 实时预览 | `$table{...}` |
| `$card{api.case.list}` | `cat2bug_case_card` | 用例卡片占位块 | `$card{...}` |
| `$image["style":"..."]{path}` | `cat2bug_dynamic_image` | 动态图片块 + 实时预览 | `$image...` |
| 计划统计大表、multimd 合并表等 | `cat2bug_raw_block` | 只读保留块 + 源码编辑提示 | 原文逐行透传 |
| 标准 GFM（标题、列表、普通表格等） | Crepe / GFM 内置节点 | 常规 WYSIWYG | GFM Markdown |

## 工具栏与插入内容

工具栏菜单定义仍源自 `Cat2BugMarkdown/MarkdownTools.js`（经 `toolbarMenuLoader` 加载），插入字符串由 `useReportTemplateTools.resolveToolContent` 解析后写入文档。

| 工具 | 插入示例 |
|------|----------|
| 左/中/右对齐 | `:-- 文字`、`-:- 文字`、`--: 文字` |
| 下划线 / 删除线 / 高亮 | `++文字++`、`~~文字~~`、`==文字==` |
| 上标 / 下标 | `^文字^`、`~文字~` |
| 缺陷表 | `$table{api.defect.list[14 列默认]}` |
| 计划统计大表 | `buildPlanStatisticsTable(n)` 生成的宽表（保留块） |

## 视图模式

| 模式 | 说明 |
|------|------|
| 可视化 | Milkdown 编辑，DSL 块可视化 |
| 源码 | `MdEditorPane` 纯文本编辑 |
| 可视化和源码 | 左 Milkdown + 右源码对照 |

## 测试与回归

- 单元测试：`tests/unit/report-template/dsl-roundtrip.spec.js`、`complex-blocks.spec.js`、`toolbar-insert.spec.js`、`export-regression.spec.js`
- E2E：`e2e/report/template-edit.spec.cjs`
- E4 对比样例：`tests/fixtures/report-template/phase0-e4-viewreport-golden.md`
- DSL 使用统计（可选）：`scripts/scan-report-template-dsl.cjs`

## 相关文件

```
src/components/ReportTemplateEditor/
├── dsl/                    # parse / serialize / complex-blocks
├── milkdown/               # Crepe 插件、NodeView、remark
├── composables/            # 工具栏、计划菜单
└── ReportTemplateEditor.vue
```

渲染与导出（不在本编辑器内实现）：

```
src/components/Cat2BugMarkdown/plugins/   # TablePlugin, VariablePlugin, ...
src/components/Report/ViewReport.vue
```
