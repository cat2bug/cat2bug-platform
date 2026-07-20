---
name: cat2bug-report-skills-index
description: >-
  Index for Cat2Bug report template AI skills: national test standards (GB/T 38634 /
  ISO 29119), Cat2Bug Markdown DSL variables, and default templates. Use when
  authoring or reviewing test report templates.
---

# Cat2Bug 报告模版 Skills 索引

## 何时加载

| 场景 | 先读 | 再读 |
|------|------|------|
| 写/改报告模版 DSL | `cat2bug-report-dsl/SKILL.md` | `variables-reference.md` 等 |
| 对齐国标文档结构 | 本索引 + `standards/gb-t-38634-overview.md` | 仅打开任务相关 `_sources` 切片 |
| 选默认模版形态 | `report-templates/taxonomy.md` | `report-templates/defaults/*` |

## 国标裁剪原则（摘要）

- 模版边界**跟从** GB/T 38634.3 / ISO 29119-3 对信息项与文档的定义及 full/tailored 符合性。
- **禁止**机械「一类文档名 = 必须单独一个模版文件」。
- 同一文档内的必选信息元素应落在同一主模版（或标明的附录）；独立交付物才分模版。

## 分段读取协议（强制）

国标 PDF/OCR **体量大**。约定：

1. **读全** = 磁盘上 OCR/抽取完整落盘，**不是**一次灌进模型上下文。
2. 先读本索引与各册 `manifest.json`，再只打开与当前任务相关的 **1～N 个切片**（建议单次不超过一章或约数十页文本）。
3. Skills 正文只保留结构化摘要；细节用「路径 + 页码」指向 `_sources/`。
4. 多 agent 按**册或章**分治；禁止单 agent 通读四本 GB。
5. 需要核对原文时再追加相邻切片。

### 源路径

- GB OCR：`readme/development/skills/_sources/ocr/gb-t-38634-{1,2,3,4}/`
- ISO 29119-3：`readme/development/skills/_sources/iso-29119-3/`

## 目录地图

```
readme/development/skills/
├── SKILL-INDEX.md                 ← 本文件
├── _sources/                      ← OCR / ISO 切片（按需读）
├── standards/                     ← 国标摘要 skills
├── cat2bug-report-dsl/            ← DSL / 变量 skills
└── report-templates/              ← taxonomy + defaults/
```

## Standards skills（链接）

| 文件 | 用途 |
|------|------|
| `standards/gb-t-38634-overview.md` | GB/T 38634 ↔ ISO 29119 映射、OCR 状态、读取协议 |
| `standards/test-documentation-29119-3.md` | Part 3 信息项、公共头、full/tailored；指针至 `_sources/iso-29119-3/` |
| `standards/test-process-concepts-techniques.md` | Part 1/2/4（概念/过程/技术）短摘要 |

## Report-template skills（链接）

| 文件 | 用途 |
|------|------|
| `report-templates/taxonomy.md` | 信息项 → standalone / combined / appendix / skeleton-only；推荐 `defaults/*` 文件名 |
| `report-templates/defaults/README.md` | 默认模版库存一览 |
| `report-templates/defaults/test-status-report.md` | 测试状态报告（P0） |
| `report-templates/defaults/test-completion-report.md` | 测试完成报告（P0） |
| `report-templates/defaults/test-plan.md` | 测试计划（P0） |
| `report-templates/defaults/test-incident-report.md` | 测试事件/缺陷报告（P0） |
| `report-templates/defaults/test-specification.md` | 测试说明 8.2–8.4（P1） |
| `report-templates/defaults/test-data-readiness-report.md` | 测试数据就绪（P2） |
| `report-templates/defaults/test-environment-readiness-report.md` | 测试环境就绪（P2） |
| `report-templates/defaults/skeleton-test-policy.md` | 测试方针骨架（P3） |
| `report-templates/defaults/skeleton-organizational-test-practices.md` | 组织级测试实践骨架（P3） |

## Cat2Bug DSL skills（链接）

| 文件 | 用途 |
|------|------|
| `cat2bug-report-dsl/SKILL.md` | DSL 入口 |
| `cat2bug-report-dsl/variables-reference.md` | 变量与计划下标字段 |
| `cat2bug-report-dsl/tables-images-cards.md` | `$table` / `$image` / `$card` |
| `cat2bug-report-dsl/authoring-rules.md` | 编写约束 |

## 加载顺序建议（写默认模版）

1. `cat2bug-report-dsl/SKILL.md` + `authoring-rules.md`
2. `report-templates/taxonomy.md`
3. 按 taxonomy 打开 **一个** 相关标准切片（如完成报告章节）
4. 编写 `defaults/*.md`（仅用已文档化 DSL）
