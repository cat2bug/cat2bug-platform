---
name: iso-9001-gbt-19001-qms
description: >-
  ISO 9001:2015 / GB/T 19001-2016 QMS skills: clause intent, certification context,
  Cat2Bug evidence mapping, record templates vs 29119-3 test documentation.
  Use when authoring QMS manuals/records or explaining audit evidence from Cat2Bug.
---

# 质量管理体系（ISO 9001 / GB/T 19001）Skills 入口

## 何时加载本 skill

| 场景 | 加载 |
|------|------|
| 编写/审查 **QMS 成文信息或记录**（方针、程序、内审、管评、纠正措施等） | 本 skill + `overview-iso-9001-gbt-19001.md` |
| 说明 Cat2Bug 在认证审核中可提供的 **客观证据** | + `cat2bug-qms-mapping.md` |
| 同时涉及 **测试文档** 与 **QMS** | + `vs-29119-3.md`，测试模版走 `../29119-3/` |
| 仅写测试报告 DSL 模版 | **不要**以本 skill 替代 `../29119-3/`；见 [`../../SKILL-INDEX.md`](../../SKILL-INDEX.md) |

## 与 29119-3 的分工

- **29119-3 / GB/T 38634.3**：规定测试过程产出的 **测试文档信息项**（计划、说明、报告等）。
- **9001 / GB/T 19001**：规定 **质量管理体系** 的过程与 **成文信息**（含须保留的记录）。
- 同一审核可能两类都要：例如「测试完成报告」来自测试标准；「该报告是否经评审批准」来自 QMS 记录。详见 [vs-29119-3.md](./vs-29119-3.md)。

## PDCA 与条款 4–10（工作记忆）

ISO 9001:2015 用 **策划—实施—检查—处置** 循环组织要求；条款 4–10 可粗映射为：

| 循环 | 主要条款 | Agent 写模版时的关注点 |
|------|----------|------------------------|
| 策划 | 4 组织背景，6 策划 | 范围、相关方、风险机遇、质量目标 |
| 实施 | 5 领导，7 支持，8 运行 | 职责、资源、成文信息控制、设计开发/生产服务/外购等运行控制 |
| 检查 | 9 绩效评价 | 监视测量、内审、管理评审 |
| 处置 | 10 改进 | 不符合、纠正措施、持续改进 |

软件测试组织常把 **GB/T 38634 / 29119** 嵌入条款 8 的运行控制；本目录补 **QMS 侧** 的记录与程序，不重复测试信息项结构。

## 成文信息 vs 记录

| 概念 | 写模版时 |
|------|----------|
| **成文信息**（documented information） | 组织需 **保持** 的体系文件：手册、程序、作业指导、外来文件清单等；版本、审批、分发在 QMS 程序中规定 |
| **记录**（records） | 为证明 **已发生** 的活动而 **保留** 的证据：签到、检查表、评审纪要、措施闭环；通常命名含日期/编号 |
| Cat2Bug | 缺陷、用例、计划、报告、评论/批注、成员与权限等多为 **运行与测试证据**；写入 QMS 记录时注明导出范围与责任人，**勿虚构** `${api.*}` 字段 |

QMS 记录模版优先 **（人工填写）**、**【待填写】** 占位；仅当任务明确且变量已在 [`../../cat2bug-report-dsl/variables-reference.md`](../../cat2bug-report-dsl/variables-reference.md) 中文档化时，才在 **测试类** 附件模版中使用 DSL。

## 指针列表（按任务打开）

| 文件 | 内容 |
|------|------|
| [README.md](./README.md) | 目录清单、加载顺序 |
| [overview-iso-9001-gbt-19001.md](./overview-iso-9001-gbt-19001.md) | 标准等价性、条款 4–10 中文意图、认证阶段概要 |
| [cat2bug-qms-mapping.md](./cat2bug-qms-mapping.md) | 证据需求 ↔ 产品能力 ↔ 缺口 |
| [vs-29119-3.md](./vs-29119-3.md) | 测试文档 vs QMS 记录对照示例 |
| `manuals/` | 体系文件，见 [manuals/README.md](./manuals/README.md) |
| `records/` | QMS 记录模版，见 [records/README.md](./records/README.md) |

## 编写约束

1. 标题用 **plain 中文**，不写「可选」类模糊章节名。
2. 不抄写任何 copyrighted 解读书的段落、表格或长摘录。
3. 不发明未文档化的 Cat2Bug DSL 变量。
4. 缺口（培训 LMS、完整 CAPA、管评模块等）在模版中留栏位并指向 **组织外部流程**，见 `cat2bug-qms-mapping.md`。
