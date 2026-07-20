# ISO 9001 / GB/T 19001 质量管理体系 Skills 清单

路径根：`readme/development/skills/report-templates/9001/`  
与测试文档模版目录 [`../29119-3/`](../29119-3/README.md) 并列：本目录面向 **QMS 认证与成文信息/记录**，不替代 GB/T 38634.3 测试信息项模版。

## 何时用本目录 vs 29119-3

| 目标 | 目录 |
|------|------|
| 测试计划、用例说明、状态/完成报告、测试事件等 **测试文档** | `29119-3/` |
| 质量手册、程序文件、评审记录、内审/管理评审/纠正措施等 **QMS** | `9001/`（本目录） |
| 审核既要测试交付物又要 QMS 证据 | 两者都加载；分工见 [vs-29119-3.md](./vs-29119-3.md) |

## 库存一览

### Skills（Agent 必读）

| 文件 | 用途 |
|------|------|
| [SKILL.md](./SKILL.md) | 入口：加载顺序、PDCA、成文信息与记录、指针列表 |
| [overview-iso-9001-gbt-19001.md](./overview-iso-9001-gbt-19001.md) | 标准对照、条款 4–10 意图摘要、认证周期概要 |
| [cat2bug-qms-mapping.md](./cat2bug-qms-mapping.md) | QMS 证据需求 ↔ Cat2Bug 能力（含缺口） |
| [vs-29119-3.md](./vs-29119-3.md) | 与测试文档 skills 的边界与并用场景 |

### Manuals（质量手册与程序文件）

见 [manuals/README.md](./manuals/README.md)。含质量手册及成文信息、设计开发、测试验证、不符合与纠正、内审、管理评审、能力培训、风险机遇等程序。

### Records（过程记录模版）

见 [records/README.md](./records/README.md)。含测试用例/需求/设计/测试计划评审、培训、不符合与纠正、内审检查、管理评审纪要、变更、顾客投诉、供方评价、能力矩阵等。

## 加载顺序（Agent）

1. [SKILL.md](./SKILL.md)
2. 任务相关：[overview-iso-9001-gbt-19001.md](./overview-iso-9001-gbt-19001.md) 和/或 [cat2bug-qms-mapping.md](./cat2bug-qms-mapping.md)
3. 写手册/程序 → [manuals/](./manuals/)；写记录 → [records/](./records/)
4. 若同时编写测试报告模版：先读 [vs-29119-3.md](./vs-29119-3.md)，再加载 [`../../cat2bug-report-dsl/SKILL.md`](../../cat2bug-report-dsl/SKILL.md) 与 [`../29119-3/`](../29119-3/README.md)

## 规范性文本说明

- **ISO 9001:2015** 与 **GB/T 19001-2016** 条款编号一致（4–10）；逐字要求须购买正版标准。
- 本仓库 **不存放** 受版权限制的解读类全书 PDF；GB 扫描件若无可检索文本，亦不以 OCR 切片形式纳入 `_sources/`。
- Skills 正文为 **原创中文摘要**，供 Agent 写模版与映射证据，不可替代标准原文。

## 参考文档

- ISO 9001:2015 — Quality management systems — Requirements  
- GB/T 19001-2016 — 质量管理体系 要求  
- 测试文档对照：[`../../standards/gb-t-38634-overview.md`](../../standards/gb-t-38634-overview.md)
