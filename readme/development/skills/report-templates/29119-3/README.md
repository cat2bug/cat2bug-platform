# 国标默认报告模版清单

路径根：`readme/development/skills/report-templates/29119-3/`  
形态定义见 [../taxonomy.md](../taxonomy.md)。信息项与标准的对应关系也写在 taxonomy 中；**模版正文本身按独立项目文档编写**，不嵌入标准条款编号。

DSL 编写须遵守 [../../cat2bug-report-dsl/SKILL.md](../../cat2bug-report-dsl/SKILL.md)。

## 库存一览

| 优先级 | 文件 | 形态 | 用途 |
|--------|------|------|------|
| P0 | [test-status-report.md](./test-status-report.md) | **standalone** | 测试状态报告（报告期内进展与度量） |
| P0 | [test-completion-report.md](./test-completion-report.md) | **standalone** | 测试完成报告 / 测试总结报告 |
| P0 | [test-plan.md](./test-plan.md) | **standalone** | 测试计划 |
| P0 | [test-incident-report.md](./test-incident-report.md) | **standalone** | 测试事件报告；缺陷清单对接 Cat2Bug 缺陷表 |
| P1 | [test-specification.md](./test-specification.md) | **combined sections** | 测试说明（模型 / 用例 / 规程合订） |
| P2 | [test-data-readiness-report.md](./test-data-readiness-report.md) | **standalone**（短文） | 测试数据就绪报告 |
| P2 | [test-environment-readiness-report.md](./test-environment-readiness-report.md) | **standalone**（短文） | 测试环境就绪报告 |
| P3 | [skeleton-test-policy.md](./skeleton-test-policy.md) | **skeleton-only** | 测试方针（组织级） |
| P3 | [skeleton-organizational-test-practices.md](./skeleton-organizational-test-practices.md) | **skeleton-only** | 组织级测试实践 |

## 未纳入首期默认正文（taxonomy 可选）

| 文件 | 形态 | 说明 |
|------|------|------|
| `test-procedure-specification.md` | standalone / 从 test-specification 拆出 | 组织分册管理规程时使用 |
| `test-data-requirements.md` | appendix 或计划内章节 | 可并入 test-plan / test-specification |
| `test-environment-requirements.md` | appendix 或计划内章节 | 同上 |
| `skeleton-test-execution-log.md` | skeleton / 工具导出 | 优先工具记录 |

## 导入与定制

- 产品 classpath 预制由仓库根目录 `./scripts/sync-report-preset-templates.sh` 同步到 `cat2bug-platform-admin/src/main/resources/报告/模版/`（含 `manifest.json` 与预览 PNG）；修改本目录正文后请重跑该脚本。
- 将上述 `.md` 复制到项目的报告模版库后，可按组织需要删减章节并记录理由。
- 计划相关变量默认使用 **1-based** 下标 `[1]`，与报告所选测试计划序号一致；多计划场景可增改 `[2]` 等。
- 标准出处仅在各模版文末「参考文档」中标明；编写与阅读时以本文件章节标题为准。

## 参考文档

- ISO/IEC/IEEE 29119-3 Software and systems engineering — Software testing — Part 3: Test documentation  
- GB/T 38634.3（与上对照）  
- 信息项 → 文件映射：[../taxonomy.md](../taxonomy.md)
