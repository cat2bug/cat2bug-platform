# 测试与验证控制程序

> 与 ISO/IEC/IEEE 29119-3 测试文档模版及 Cat2Bug 运行证据对接。

## 文档标识

| 项 | 内容 |
|----|------|
| 唯一标识 | （人工填写） |
| 版本 | （人工填写） |
| 状态 | （人工填写） |
| 批准 | （人工填写：批准人、日期） |

---

## 目的

（人工填写）规定测试与验证的策划、用例设计与评审、环境与数据就绪、执行、缺陷管理、报告与准出要求，为产品符合性提供客观证据。

## 范围

（人工填写）适用的测试级别（单元、集成、系统、验收等）、项目类型及与生产/发布的关系。

## 职责

| 角色 | 职责 |
|------|------|
| 测试经理/负责人 | （人工填写） |
| 测试工程师 | （人工填写） |
| 开发/设计代表 | （人工填写）缺陷修复与回归配合 |
| 项目经理 | （人工填写）资源与准出决策参与 |
| 质量负责人 | （人工填写）独立监督（如适用） |

---

## 程序步骤

### 测试策划

（人工填写）测试范围、策略、资源、进度、入口/出口准则、风险；使用模版 [../29119-3/test-plan.md](../29119-3/test-plan.md) 或组织等效表单；在 Cat2Bug 中维护计划与里程碑（如适用）。

### 测试用例设计与评审

（人工填写）依据需求/设计输出追溯；评审参与方与通过准则；模版 [../29119-3/test-specification.md](../29119-3/test-specification.md)；组织级实践可参考 [../29119-3/skeleton-organizational-test-practices.md](../29119-3/skeleton-organizational-test-practices.md)。

### 测试环境与数据就绪

（人工填写）环境配置、基线、数据脱敏与代表性；就绪检查与签字；模版 [../29119-3/test-environment-readiness-report.md](../29119-3/test-environment-readiness-report.md)、[../29119-3/test-data-readiness-report.md](../29119-3/test-data-readiness-report.md)。

### 测试执行

（人工填写）执行记录、阻塞与重测规则、自动化与手工分工；Cat2Bug 中用例执行状态与附件要求。

### 缺陷管理

（人工填写）登记、分级、分配、修复、回归、关闭准则；与不符合处置的接口；事件/事故模版 [../29119-3/test-incident-report.md](../29119-3/test-incident-report.md)（如适用）。

### 测试报告与状态沟通

（人工填写）周期状态与完成总结；模版 [../29119-3/test-status-report.md](../29119-3/test-status-report.md)、[../29119-3/test-completion-report.md](../29119-3/test-completion-report.md)。

### 准出与放行

（人工填写）出口准则（通过率、遗留缺陷等级、风险接受）；准出批准权限；与设计开发确认、管理评审输入的关系。

### 记录与归档

（人工填写）Cat2Bug 导出、PDF/受控记录编号、保存期限；本目录 `../records/` 中 QMS 记录与 29119-3 项目级模版的职责划分。

---

## 相关记录

| 记录名称 | 路径 |
|----------|------|
| 测试计划批准记录 | [../records/record-test-plan-approval.md](../records/record-test-plan-approval.md) |
| 用例评审记录 | [../records/record-test-case-review.md](../records/record-test-case-review.md) |
| 环境/数据就绪签字 | [../records/record-test-readiness-signoff.md](../records/record-test-readiness-signoff.md) |
| 测试准出记录 | [../records/record-test-exit-criteria.md](../records/record-test-exit-criteria.md) |

---

**参考文档：** ISO 9001:2015 运行控制、监视测量与产品和服务放行相关主题；GB/T 19001-2016；ISO/IEC/IEEE 29119-3 测试文档（见目录 [../29119-3/README.md](../29119-3/README.md)）；GB/T 38634.3 测试文档对照（组织可选）。
