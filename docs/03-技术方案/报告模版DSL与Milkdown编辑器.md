# 报告模版 DSL 与 Milkdown 编辑器

说明报告模版的存储格式（Markdown + Cat2Bug DSL）、可视化编辑器（Milkdown）与渲染链路的分工，以及预制国标模版的打包方式；改报告模版、编辑器或新增 DSL token 时参考。

## 概述

- 模版正文存于 `sys_report_template.template_content`，**始终是纯文本 Markdown + DSL token**，编辑器不得引入 JSON / 二进制格式。
- **编辑**：`ReportTemplateEditor`（基于 Milkdown 7 / Crepe）把 DSL 解析为自定义节点做所见即所得编辑，保存时序列化回原文。
- **渲染**：报告预览、Word / PDF 导出仍走 `markdown-it` 插件链（`Cat2BugMarkdown/plugins/`），与编辑器解耦。
- DSL 语法的权威说明在 `readme/development/skills/cat2bug-report-dsl/`，DSL ↔ 节点映射表在 `readme/design-system/report-template-dsl-mapping.md`，本篇不重复。

## 关键流程

1. **DSL 形态**（详见规格）：变量 `${api.<ns>.<field>}`、计划下标变量 `${api.plan.<field>[n]}`（n 从 1 开始）、动态表 `$table{api.defect.list[...]}` / `$table{api.case.list[...]}`、卡片 `$card{api.case.list}`、图片 `$image["style":"..."]{api.project.projectIcon}`，以及对齐标记与 `++下划线++`、`==标记==` 等扩展行内语法。
2. **解析 / 序列化**：`dsl/grammar.js` 定义 token 与默认列；`dsl/parse.js`、`dsl/serialize.js` 负责往返；remark 插件 `milkdown/remark-cat2bug-dsl*.js` 把 token 转为节点。计划统计大表、multimd 合并表等暂未建模的结构作为 `cat2bug_raw_block` **逐字保留**。
3. **编辑器节点**：`cat2bug_variable`、`cat2bug_plan_variable`、`cat2bug_defect_table`、`cat2bug_case_card`、`cat2bug_dynamic_image`、`cat2bug_raw_block`（定义在 `milkdown/cat2bug-nodes.js`，视图在 `milkdown/cat2bug-views.js`、`nodes/`）。
4. **变量显示**：默认以「内容模式」（解析后的值）显示且无边框；hover 时浮出虚线框、标题行与双态切换（内容 / 原始 token），浮层不得引起布局位移；工具栏有「全部展开 / 全部折叠」，默认全部展开。展开状态只影响显示，不改变序列化结果。
5. **预制模版**：ISO/IEC/IEEE 29119-3 与 ISO 9001 两组预制模版以 classpath 资源 `报告/模版/`（Markdown + 预览 PNG + `manifest.json`）打包，接口 `/system/report/template/preset/{group}/[{folder}/]{key}/content|preview|copy`。预制模版只能复制或直接用于生成报告，不能编辑删除；选择器顺序为「项目自定义 → 29119-3 → ISO 9001」。

## 关键代码位置

- 编辑页：`cat2bug-platform-ui/src/views/system/report/template/EditReportTemplate.vue`（异步加载 `ReportTemplateEditor`）
- 编辑器：`cat2bug-platform-ui/src/components/ReportTemplateEditor/`（`dsl/`、`milkdown/`、`nodes/`、`composables/`、`ReportTemplateToolbar.vue`、`PreviewPane.vue`、`MarkdownSourcePane.vue`）
- 渲染插件：`cat2bug-platform-ui/src/components/Cat2BugMarkdown/plugins/`（`VariablePlugin`、`PlanVariablePlugin`、`TablePlugin`、`CaseCardPlugin`、`CardPlugin`、`ImagePlugin`、对齐插件）
- 报告查看：`cat2bug-platform-ui/src/components/Report/ViewReport.vue`
- 预制模版后端：`cat2bug-platform-admin/src/main/java/com/cat2bug/admin/service/report/ReportPresetTemplateService.java`、`resource/ReportTemplateResource.java`
- 预制模版资源：`cat2bug-platform-admin/src/main/resources/报告/模版/`
- 同步脚本：`scripts/sync-report-preset-templates.sh` 与 `scripts/report-preset/*`（源：`readme/development/skills/report-templates/`，写入上述资源目录）

## 注意事项

- 新增 token 必须同时改三处：`dsl/grammar.js`（解析/序列化）、对应 markdown-it 插件（渲染）、`readme/development/skills/cat2bug-report-dsl/`（技能文档）；AI 生成模版只允许用技能中登记的 token。
- 未建模的 DSL 必须原样往返，不能被编辑器「规范化」掉；改 parse/serialize 要补往返单测。
- 不要手改 `报告/模版/` 下的 Markdown 与 PNG，改源文件后跑同步脚本。同步脚本写入 `cat2bug-platform-admin/src/main/resources/报告/模版/`（原先只写 Spring admin、需另行同步 Quarkus 副本的问题已随 Spring 线删除而消失）。
- Native 构建需把 `报告/**` 列入 `quarkus.native.resources.includes`（`application.properties` 已包含，`application-native.properties` 的列表中未出现，见待确认）。

## 相关规格

- `openspec/specs/report-template-dsl/spec.md`
- `openspec/specs/report-template-milkdown-editor/spec.md`
- `openspec/specs/report-template-variable-preview/spec.md`
- `openspec/specs/report-template-preset-groups/spec.md`
- `openspec/specs/cat2bug-report-dsl-skills/spec.md`、`openspec/specs/national-standard-default-templates/spec.md`
- `openspec/specs/report-detail-layout/spec.md`、`openspec/specs/report-annotation/spec.md`

## 待确认

- `application-native.properties` 中 `quarkus.native.resources.includes` 未包含 `报告/**`、`i18n/**`、`defaults/**`，Native 包里预制模版是否可用。
