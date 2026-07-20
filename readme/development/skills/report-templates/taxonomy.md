---
name: report-template-taxonomy
description: >-
  Map ISO 29119-3 / GB/T 38634.3 information items to template shapes
  (standalone | combined sections | appendix | skeleton-only) and recommended
  default files. Follows design D1 — boundaries follow the standard.
---

# 报告模版 Taxonomy（跟从标准，非一类一文件）

依据：ISO/IEC/IEEE 29119-3:2021（GB/T 38634.3 对照；GB 措辞见 `_sources/ocr/gb-t-38634-3/`）。  
原则（design **D1**）：模版边界跟从标准对文档/信息项与 full·tailored 的定义；**禁止**机械「一类一名一份文件」。

符合性允许：电子记录、分册、或**合并进同一文档**；标题版式可改（`05-clauses-1-4.md` §4.1.1）。

## 形态图例

| 形态 | 含义 |
|------|------|
| **standalone** | 默认独立主模版文件（常作独立交付物） |
| **combined sections** | 多个信息元素作为**同一文档内章节**；只出一份主模版 |
| **appendix** | 不单独成默认交付模版；作为主模版附录/附表，或由工具记录引用 |
| **skeleton-only** | 仅结构骨架（少/无动态变量）；长文政策类不强制做成完整动态模版 |

「推荐独立」与「允许并入」可并存：默认给推荐形态；tailored 合并须在项目中记录理由。

## 组织级（Clause 6）

| 信息项 | 形态 | 标准依据（摘要） | 推荐默认文件 |
|--------|------|------------------|--------------|
| Test policy | **skeleton-only** | 组织级、非项目专用（术语 3.20）；design 默认不做完整动态模版 | `defaults/skeleton-test-policy.md` |
| Organizational test practices | **skeleton-only**（可与方针合并） | 3.5：可无独立方针而并入实践；可有多份实践覆盖不同语境 | `defaults/skeleton-organizational-test-practices.md` |

**禁止**：把方针/实践拆成与 Clause 6 无关的多份「伪报告」模版却声称国标一类一文件。

## 测试管理（Clause 7）

| 信息项 | 形态 | 标准依据（摘要） | 推荐默认文件 |
|--------|------|------------------|--------------|
| Test plan | **standalone**（多计划时多实例，非多类型文件） | §7.1 / 7.2：独立信息项；大项目可有项目计划 + 级别/类型计划（3.19） | `defaults/test-plan.md` |
| Test status report | **standalone** | §7.1 / 7.3：独立信息项；敏捷口头站会且无记录 → tailored（7.3.1 Note） | `defaults/test-status-report.md` |
| Test completion report | **standalone** | §7.1 / 7.4：独立信息项（亦称 test summary report，3.9） | `defaults/test-completion-report.md` |

**combined sections（文档内部）**：计划的 7.2.2–7.2.10、状态报告 7.3.2–7.3.8、完成报告 7.4.2–7.4.10 均为**同一文档内章节** → 各用**一份**主模版容纳全部必选槽位；不要把「风险登记」「测试策略」等拆成无依据的独立默认模版。

**允许并入（tailored）**：状态/完成摘要并入项目管理周报等——须按 §4.1.3 记录子集与理由；taxonomy 默认仍保留独立文件以便合规交付。

无组织级实践时，计划可**纳入**实践中有而计划模板未重复的章节（7.2.1）→ 作为计划模版内可选块，而非强制第二文件。

## 动态测试（Clause 8）

| 信息项 | 形态 | 标准依据（摘要） | 推荐默认文件 |
|--------|------|------------------|--------------|
| Test model specification | **combined sections** → 测试说明 | 同属 test specification 组成部分（3.23） | （见下行合并） |
| Test case specification | **combined sections** → 测试说明 | 3.23：可为单文档或多文档 | `defaults/test-specification.md`（含模型/用例/规程章节） |
| Test procedure specification | **combined sections** → 测试说明 | 同上；亦可另存为脚本库（允许分册） | 同上；可选拆分 `defaults/test-procedure-specification.md` |
| Test data requirements | **appendix** 或计划内章节 | 可引用 OTP / 计划 / 测试说明中的位置（3.13 同类引用模式） | 附录块于 `test-plan.md` 或 `test-specification.md`；可选 `defaults/test-data-requirements.md` |
| Test environment requirements | **appendix** 或计划内章节 | 同上 | 同上；可选 `defaults/test-environment-requirements.md` |
| Test data readiness report | **standalone**（短）或 **appendix** | 独立信息项（3.10）；体量小，可附于状态报告 | `defaults/test-data-readiness-report.md`（或并入状态报告附录） |
| Test environment readiness report | **standalone**（短）或 **appendix** | 独立信息项（3.12） | `defaults/test-environment-readiness-report.md` |
| Actual results | **appendix** / 工具记录 | 常与执行工具绑定；§8.9 | 不强制默认正文模版；需要时附录于执行包 |
| Test result | **appendix** / 工具记录 | 通过/失败指示（3.22） | 同上 |
| Test execution log | **appendix** / 工具记录 | 独立信息项但多为系统日志 | `defaults/skeleton-test-execution-log.md`（可选） |
| Test incident report | **standalone** | 独立信息项（3.4 / §8.11）；产品缺陷单常对应该形态 | `defaults/test-incident-report.md` |

**Test specification 默认策略**：首期推荐**一份** `test-specification.md` 覆盖 8.2–8.4（combined sections）。若组织已分册管理用例与规程，允许拆成独立文件（标准 3.23 明确允许多文档）——属推荐独立之外的允许分册，不是「必须一类一文件」。

## 推荐默认模版清单（首期）

按交付优先级（正文见 `defaults/`；清单见 `defaults/README.md`）：

| 优先级 | 文件 | 形态 |
|--------|------|------|
| P0 | `defaults/test-completion-report.md` | standalone |
| P0 | `defaults/test-status-report.md` | standalone |
| P0 | `defaults/test-plan.md` | standalone |
| P0 | `defaults/test-incident-report.md` | standalone |
| P1 | `defaults/test-specification.md` | combined（8.2–8.4） |
| P2 | `defaults/test-data-readiness-report.md` | standalone 短文 |
| P2 | `defaults/test-environment-readiness-report.md` | standalone 短文 |
| P3 | `defaults/skeleton-test-policy.md` | skeleton-only |
| P3 | `defaults/skeleton-organizational-test-practices.md` | skeleton-only |
| 可选 | `defaults/test-procedure-specification.md` | 从 test-specification 拆出时用 |
| 可选 | `defaults/test-data-requirements.md` / `test-environment-requirements.md` | 不放进计划附录时用 |
| 可选 | `defaults/skeleton-test-execution-log.md` | skeleton / 工具导出说明 |

路径根：`readme/development/skills/report-templates/`。

## 决策速查

1. 标准把若干元素写成**同一文档的章节** → **一份**主模版（combined sections），可加标明的 appendix。  
2. 标准定义为**不同信息项/交付物** → 默认分模版（standalone），除非 tailored 合并并记录理由。  
3. 组织级长文政策 → **skeleton-only**。  
4. 执行期结果/日志 → 优先工具记录 + appendix，勿为「凑类型」复制空壳 standalone。  
5. 写正文前打开**一个**相关切片（如完成报告 → `11-section-7.4.md` + 必要时 `23-annex-a.md`），并遵守 `../cat2bug-report-dsl/`；**勿发明 DSL**。

## 指针

- 文档 skill：`../standards/test-documentation-29119-3.md`
- ISO 切片清单：`../_sources/iso-29119-3/manifest.json`
- 设计 D1：`openspec/changes/national-standard-report-templates/design.md`
