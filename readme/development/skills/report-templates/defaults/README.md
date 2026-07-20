# 国标默认报告模版清单

路径根：`readme/development/skills/report-templates/defaults/`  
形态定义见 [../taxonomy.md](../taxonomy.md)（ISO/IEC/IEEE 29119-3 / GB/T 38634.3，design D1）。

DSL 编写须遵守 [../../cat2bug-report-dsl/SKILL.md](../../cat2bug-report-dsl/SKILL.md)。

## 库存一览

| 优先级 | 文件 | 形态 | 标准依据（摘要） |
|--------|------|------|------------------|
| P0 | [test-status-report.md](./test-status-report.md) | **standalone** | Clause 7.3 测试状态报告（7.3.2–7.3.8 合并于同一文档） |
| P0 | [test-completion-report.md](./test-completion-report.md) | **standalone** | Clause 7.4 测试完成报告 / 测试总结报告（7.4.2–7.4.10） |
| P0 | [test-plan.md](./test-plan.md) | **standalone** | Clause 7.2 测试计划（7.2.2–7.2.10） |
| P0 | [test-incident-report.md](./test-incident-report.md) | **standalone** | Clause 8.11 测试事件报告；缺陷清单对接 Cat2Bug 缺陷表 |
| P1 | [test-specification.md](./test-specification.md) | **combined sections** | Clause 8.2–8.4 测试说明（模型 / 用例 / 规程） |
| P2 | [test-data-readiness-report.md](./test-data-readiness-report.md) | **standalone**（短文） | Clause 8.7 测试数据就绪报告 |
| P2 | [test-environment-readiness-report.md](./test-environment-readiness-report.md) | **standalone**（短文） | Clause 8.8 测试环境就绪报告 |
| P3 | [skeleton-test-policy.md](./skeleton-test-policy.md) | **skeleton-only** | Clause 6.2 测试方针 |
| P3 | [skeleton-organizational-test-practices.md](./skeleton-organizational-test-practices.md) | **skeleton-only** | Clause 6.3 组织级测试实践 |

## 未纳入首期默认正文（taxonomy 可选）

| 文件 | 形态 | 说明 |
|------|------|------|
| `test-procedure-specification.md` | standalone / 从 test-specification 拆出 | 组织分册管理规程时使用 |
| `test-data-requirements.md` | appendix 或计划内章节 | 8.5；可并入 test-plan / test-specification |
| `test-environment-requirements.md` | appendix 或计划内章节 | 8.6；同上 |
| `skeleton-test-execution-log.md` | skeleton / 工具导出 | 8.10；优先工具记录 |

## 导入与定制

- 将上述 `.md` 复制到项目的报告模版库后，可按 tailored 符合性删减章节并记录理由（标准 §4.1.3）。
- 计划相关变量默认使用 **1-based** 下标 `[1]`，与报告所选测试计划序号一致；多计划场景可增改 `[2]` 等。
