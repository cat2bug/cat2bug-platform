# ISO 9001 / GB/T 19001 质量手册与程序

本目录提供组织级 **质量管理体系（QMS）** 文档结构模版，供寻求 ISO 9001:2015 / GB/T 19001-2016 认证且使用 **Cat2Bug** 做缺陷与测试管理的组织实施参考。内容为原创中文槽位与（人工填写）占位，**非标准条文摘录**；章节标题不使用「7.5.3」类条款编号。

配套：

- 测试文档模版：[../29119-3/](../29119-3/)（ISO/IEC/IEEE 29119-3 对齐）
- QMS 记录模版：[../records/](../records/)（程序中「相关记录」所引文件名）

## 文件清单

| 文件 | 用途（一句话） |
|------|------------------|
| [quality-manual.md](./quality-manual.md) | 质量手册：范围、方针目标、过程、职责、运行与 Cat2Bug 证据关系、绩效评价与改进、文控原则。 |
| [procedure-documented-information.md](./procedure-documented-information.md) | 成文信息控制：编制、评审、批准、版本、作废、外来文件与记录保管。 |
| [procedure-design-development.md](./procedure-design-development.md) | 设计开发控制：策划、输入、评审/验证/确认、输出、变更及与测试/缺陷接口。 |
| [procedure-testing-verification.md](./procedure-testing-verification.md) | 测试与验证：计划至准出全流程，对接 29119-3 模版与 Cat2Bug。 |
| [procedure-nonconformity-capa.md](./procedure-nonconformity-capa.md) | 不符合与纠正措施：识别、处置、原因、措施、有效性验证与关闭。 |
| [procedure-internal-audit.md](./procedure-internal-audit.md) | 内部审核：计划、实施、报告与跟踪。 |
| [procedure-management-review.md](./procedure-management-review.md) | 管理评审：输入输出、频次、纪要与决议跟踪。 |
| [procedure-competence-training.md](./procedure-competence-training.md) | 能力与培训：需求、培训、考核与记录。 |
| [procedure-risk-opportunity.md](./procedure-risk-opportunity.md) | 风险与机遇：识别、评价、措施与监视。 |

## 使用说明

1. 复制或在本仓库中直接编辑，填写「文档标识」与正文（人工填写）项。
2. 将程序与 [quality-manual.md](./quality-manual.md) 中的过程描述、职责矩阵保持一致。
3. 项目级测试交付物优先使用 [../29119-3/](../29119-3/) 模版；QMS 批准类记录使用 [../records/](../records/) 中对应模版。
4. Cat2Bug 中的计划、用例、缺陷、报告宜作为 **运行证据**；是否构成正式受控记录由《成文信息控制程序》和组织记录清单界定。

---

**参考文档：** ISO 9001:2015 质量管理体系—要求；GB/T 19001-2016；与 Cat2Bug 测试/缺陷数据作为运行证据的常见实践（组织自行裁剪）。
