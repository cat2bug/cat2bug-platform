---
name: cat2bug-report-dsl
description: >-
  Cat2Bug 报告模版 Markdown DSL 参考：变量、计划下标、动态表、图片与卡片 token 语法及字段目录。
  编写或审查报告模版、国标默认模版、报告预览渲染时使用；禁止发明未在本 skill 登记的 token。
---

# Cat2Bug 报告模版 DSL（cat2bug-report-dsl）

面向 AI / 开发者的 **报告模版 Markdown DSL** 技能包。语法与字段目录对齐前端实现：

- `cat2bug-platform-ui/src/components/ReportTemplateEditor/dsl/grammar.js`
- `cat2bug-platform-ui/src/components/Cat2BugMarkdown/plugins/*Plugin.js`
- 样例：`cat2bug-platform-ui/tests/fixtures/report-template/phase0-sample.md`

## 何时使用

- 编写或修改 **报告模版**（`.md`）中的动态占位符
- 根据国标 / ISO 文档结构映射到 **可渲染的 DSL token**
- 审查模版：确认 token 合法、命名空间正确、计划下标合法
- 回答「某个指标能不能用 `${api...}` 取到」——以本目录字段清单为准

## 何时不要使用

- 不要用本 skill 去「发明」后端尚未绑定的 `api.*` 路径
- 标准条文解读请用 `standards/` 相关 skill，本包只负责 **模版 DSL**
- 不要 OCR / 引用 PDF 原文来编造字段名

## 文档索引

| 文件 | 内容 |
|------|------|
| [variables-reference.md](./variables-reference.md) | `${api.<ns>.<field>}`、`${api.plan.<field>[n]}` 及完整字段目录 |
| [tables-images-cards.md](./tables-images-cards.md) | `$table` / `$image`·`$img` / `$card` 语法与默认列 |
| [authoring-rules.md](./authoring-rules.md) | 编写约束：禁止发明 token、计划下标 1-based、优先 `api.<ns>.<field>` |

## Token 形态速览

| 形态 | 示例 |
|------|------|
| 简单变量 | `${api.project.projectName}`、`${api.case.total}` |
| 计划下标变量 | `${api.plan.defectCount[1]}` |
| 动态表 | `$table{api.defect.list[projectNum,defectName]}` |
| 图片 | `$image["style":"width:200px;height:200px;"]{api.project.projectIcon}` 或 `$img{...}` |
| 卡片 | `$card{api.case.list}` |

## 快速示例（合法）

```markdown
# 测试报告

## 1. 项目信息
项目名称: ${api.project.projectName}
$image["style":"width:200px;height:200px;"]{api.project.projectIcon}

## 2. 测试概述
用例总数: ${api.case.total}

## 3. 附录：缺陷清单
$table{api.defect.list[projectNum,defectTypeName,defectLevel,defectName,defectStateName,moduleName,moduleVersion,defectDescribe,createBy,createTime,updateBy,updateTime,imgList,annexList]}

## 4. 计划指标
缺陷总数: ${api.plan.defectCount[1]}
```

## 权威来源

字段与默认列以 `grammar.js` 导出的常量为准：

- `PROJECT_VARIABLE_FIELDS`
- `PLAN_VARIABLE_FIELDS`
- `DEFECT_TABLE_DEFAULT_COLUMNS`
- `CASE_TABLE_DEFAULT_COLUMNS`

插件注册的数据源名（`useReportMarkdownPlugins.js` / `ViewReport.vue`）决定运行时哪些 `api.*` 路径可解析。未注册路径在预览/导出中不会有值。
