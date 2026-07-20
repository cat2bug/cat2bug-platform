---
name: test-documentation-29119-3
description: >-
  ISO 29119-3 / GB/T 38634.3 test documentation: information items, common
  headers, full vs tailored conformance. Pointers to _sources/iso-29119-3 slices.
---

# 测试文档（GB/T 38634.3 ↔ ISO 29119-3）

**对照**：GB/T 38634.3 ≈ ISO/IEC/IEEE 29119-3:2021（测试文档）。  
**规范性细节**：以 ISO 切片为准；GB 中文逐字引用打开 `_sources/ocr/gb-t-38634-3/` 对应切片（OCR 有噪声，关键条款可与 ISO 交叉验证）。

索引：`_sources/iso-29119-3/manifest.json` · 目录：`00-outline.md`

## 范围与输出关系

- Part 3 规定测试文档**模板/信息项**；这些文档是 Part 2（测试过程）过程输出。
- 切片：`05-clauses-1-4.md`（Scope，PDF pp.11–15）。

## 符合性（Clause 4）

源：`05-clauses-1-4.md`（PDF p.15）

| 概念 | 要点 |
|------|------|
| 介质与形态 | 可为电子记录、分册文档，或**合并进同一文档**；不必是纸质分册。 |
| 标题版式 | 标题、章节名、版式**可改**（增删、合并、重命名）；5–8 章用名**非强制**。 |
| Full conformance | 满足 Clause 5–8 与 Annex A 中**全部** shall。 |
| Tailored conformance | 记录所选文档子集，并对该子集满足全部 shall；对未采用的 shall **须提供理由**（直接或引用）。 |
| 主张 | 组织须声明主张 full 还是 tailored。 |

**写模版**：默认按「项目常用交付子集」设计（tailored 友好）；勿假设用户声称 full conformance。

## 公共信息元素（Clause 5）

源概要：`00-outline.md`（5.2.*）；正文切片：`06-clause-5.md`（PDF p.16）

适用于**各类**测试文档的公共头/元数据（摘要）：

| 元素 | 大纲条款 |
|------|----------|
| Unique identifier | 5.2.1 |
| Issuing organization | 5.2.2 |
| Approval authority | 5.2.3 |
| Change history | 5.2.4 |
| Status | 5.2.5 |
| Introduction | 5.2.6 |
| Scope | 5.2.7 |
| References | 5.2.8 |
| Glossary | 5.2.9 |

默认模版应在文档级保留这些槽位（可用静态占位）；细节 shall 表见 Annex A：`23-annex-a.md`。

## 信息项目录（按标准文档类型）

下列「文档类型」是标准定义的信息项；是否单独成文件见 `../report-templates/taxonomy.md`（D1）。

### 组织级 — Clause 6

| 信息项 | 切片 | PDF | 大纲要点 |
|--------|------|-----|----------|
| Test policy | `07-section-6.2.md` | p.17 | 目标、过程、组织、培训、伦理、标准、价值度量、资产归档、过程改进等 |
| Organizational test practices | `08-section-6.3.md` | pp.18–21 | 组织级实践 + 级别/类型特定实践 |

术语：无独立方针时，实践文档**可并入**方针内容（`05-clauses-1-4.md` 3.5 Note 3）。

### 测试管理 — Clause 7

| 信息项 | 切片 | PDF | 内容范围（摘要） |
|--------|------|-----|------------------|
| Test plan | `09-section-7.2.md` | pp.22–26 | 上下文、假设约束、干系人、沟通、风险、策略、活动估算、人员、进度（7.2.2–7.2.10） |
| Test status report | `10-section-7.3.md` | p.27 | 状态、报告期、相对计划进度、阻塞、度量、风险变更、下期计划（7.3.2–7.3.8） |
| Test completion report | `11-section-7.4.md` | p.28 | 执行摘要、偏差、完成评价、阻塞、度量、残余风险、交付物、可复用资产、经验教训（7.4.2–7.4.10） |

管理文档类型总览见 `09-section-7.2.md` §7.1：计划 / 状态报告 / 完成报告。

### 动态测试 — Clause 8

| 信息项 | 切片 | PDF |
|--------|------|-----|
| Test model specification | `13-section-8.2.md` | p.30 |
| Test case specification | `14-section-8.3.md` | pp.31–32 |
| Test procedure specification | `15-section-8.4.md` | pp.33–34 |
| Test data requirements | `16-section-8.5.md` | pp.35–36 |
| Test environment requirements | `17-section-8.6.md` | pp.35–36 |
| Test data readiness report | `18-section-8.7.md` | p.37 |
| Test environment readiness report | `19-section-8.8.md` | p.37 |
| Actual results / test result | `20-section-8.9.md` | p.38 |
| Test execution log | `21-section-8.10.md` | p.38 |
| Test incident report | `22-section-8.11.md` | pp.39–41 |

**Test specification**（术语 3.23）：测试设计、用例与规程的完整文档——可为**一份文档、多份文档或文档+库表混合**（`05-clauses-1-4.md`）。

概述切片：`12-clause-8-overview.md`。

## Shall / Should / May

- 动词约定：shall = 要求；should = 建议；may = 允许（`05-clauses-1-4.md` 4.1.1 Note 4）。
- 各节缩略要求表：**Annex A** → `23-annex-a.md`（PDF pp.42–46）。写模版核对 shall 时打开对应 A 节，勿通读全文。

## 示例与映射（可选）

| 用途 | 切片 |
|------|------|
| 示例总览 | `24-annex-b.md` |
| 计划/状态/完成示例 | `27-annex-e.md` … `29-annex-g.md` |
| IEEE 829 映射（已废止对照） | `41-annex-s.md` |

## 写模版规则（可执行）

1. 先定**主张的文档子集**（tailored）→ 再开 taxonomy 定独立/合并/附录/章节结构。
2. 同一信息项内的 7.x.y / 8.x.y 章节默认留在**同一主模版**；不要无标准依据拆成互不关联的多个文件。
3. 需要细节时：manifest → **一个**相关切片（必要时再加 Annex A 对应段）。
4. 国标中文章名：对照 `_sources/ocr/gb-t-38634-3/`；对外可暂用 ISO 英文信息项名 + 常用中文译名。

## 常用中文译名（对照 GB OCR）

| ISO 信息项 | 常用中文（待核对） |
|------------|-------------------|
| Test policy | 测试方针 |
| Organizational test practices | 组织级测试实践 |
| Test plan | 测试计划 |
| Test status report | 测试状态报告 |
| Test completion report | 测试完成报告（测试总结报告） |
| Test incident report | 测试事件报告（缺陷/异常报告） |
| Test case / procedure specification | 测试用例 / 测试规程说明 |
