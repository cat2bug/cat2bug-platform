# ISO 9001 / GB/T 19001 过程记录模版

路径：`readme/development/skills/report-templates/9001/records/`

本目录提供 **质量管理体系过程记录** 模版，供 ISO 9001 / GB/T 19001 内外部审核及 Cat2Bug 软件测试项目留痕使用。正文为可独立阅读的简体中文表单，以 **（人工填写）** 为主；必要时可在文中说明在 Cat2Bug 中对应用例列表、缺陷列表或计划执行数据作对照。

- 模版标题 **不含**「条款 x」式编号；标准出处仅列于各文件文末 **参考文档:**（主题参照）。
- 若导入 Cat2Bug 报告模版库，DSL 使用须遵守 [../../cat2bug-report-dsl/SKILL.md](../../cat2bug-report-dsl/SKILL.md)；本目录仅少量附录使用已文档化的 `${api.project.*}`、`${api.plan.*[1]}`、`${api.case.total}`、`$table{api.case.list…}`、`$table{api.defect.list…}`。

与国标测试 **交付报告** 的区分见 [../../29119-3/README.md](../../29119-3/README.md) 与 [../../taxonomy.md](../../taxonomy.md)；本目录侧重 **QMS 审核证据与过程留痕**，可与 29119-3 报告配合使用。

---

## 记录清单与适用程序

下表「典型支持程序」为软件组织常见 QMS 程序名称，实施时应与贵司《质量手册》及程序文件编号对齐。

| 记录模版 | 文件 | 典型支持程序 / 活动 |
|----------|------|---------------------|
| 需求评审记录 | [requirements-review-record.md](./requirements-review-record.md) | 设计开发控制程序；产品和服务要求评审 |
| 设计评审记录 | [design-review-record.md](./design-review-record.md) | 设计开发控制程序；架构 / 详细设计评审 |
| 变更控制记录 | [change-control-record.md](./change-control-record.md) | 变更控制程序；配置管理程序 |
| 测试计划评审记录 | [test-plan-review-record.md](./test-plan-review-record.md) | 测试管理程序；项目质量保证 |
| 测试用例评审记录 | [test-case-review-record.md](./test-case-review-record.md) | 测试管理程序；设计开发验证策划 |
| 培训记录 | [training-record.md](./training-record.md) | 人力资源管理程序；培训管理程序 |
| 岗位能力矩阵（摘要） | [competence-matrix-record.md](./competence-matrix-record.md) | 人力资源管理程序；岗位适任评价 |
| 供方评价记录 | [supplier-evaluation-record.md](./supplier-evaluation-record.md) | 外部提供过程控制程序；采购与外包管理 |
| 顾客投诉与反馈处理记录 | [customer-complaint-record.md](./customer-complaint-record.md) | 顾客沟通与满意度程序；服务管理 |
| 不符合项与纠正措施记录 | [nonconformity-capa-record.md](./nonconformity-capa-record.md) | 不符合与纠正措施程序；内审 / 外审跟进 |
| 内部审核检查记录 | [internal-audit-checklist-record.md](./internal-audit-checklist-record.md) | 内部审核程序 |
| 管理评审会议纪要 | [management-review-minutes.md](./management-review-minutes.md) | 管理评审程序 |

---

## 与 Cat2Bug 测试资产的配合

| 活动 | 建议 Cat2Bug 对照 | 主要记录模版 |
|------|-------------------|--------------|
| 用例设计与评审 | 用例列表、模块 | 测试用例评审记录 |
| 计划制定与评审 | 测试计划、计划项、执行计数 | 测试计划评审记录 |
| 发布前后质量 | 缺陷列表、计划缺陷统计 | 顾客投诉记录、不符合与 CAPA、管理评审输入 |
| 内审抽样 | 状态 / 完成类报告导出 | 内部审核检查记录 |

---

## 导入与归档

1. 复制所需 `.md` 至项目报告模版库或文控系统；按组织命名规则增加前缀（如 `REC-QA-`）。
2. 电子签批可替代「签名」栏，但须满足成文信息控制要求（身份、日期、防篡改）。
3. 评审 / 审核类记录应能追溯到对应的需求、设计、计划、变更或 CAPA 编号。

---

**参考文档：** GB/T 19001 质量管理体系要求（ISO 9001 对照）；本目录索引按组织过程记录独立编写，不依赖标准条款编号。
