## Why

AI 编写测试报告模版时缺少两套可执行知识：（1）GB/T 38634 / ISO/IEC/IEEE 29119 对测试文档类型、信息项与符合性的规定；（2）Cat2Bug 报告 Markdown DSL（变量、表格、图片、卡片等）的准确用法。没有这些 skills，默认模版容易偏离国标结构，或写出无法渲染的变量。需要把标准与产品 DSL 沉淀为 `readme/development/skills/`，并据此按国标要求分类、产出默认可渲染的报告模版（模版边界跟从标准，而非机械地「一类文档强制一份模版」）。

## What Changes

- OCR 读全 GB/T 38634.1–.4 扫描 PDF，与可抽取全文的 ISO/IEC/IEEE 29119-3:2021 对照，形成 AI 可消费的 standards skills
- **分段读取策略**：国标 PDF/OCR 全文体积大，禁止一次灌入整本到模型上下文；按册/章/信息项切片落盘与按需加载（见 design）
- 从现有 `ReportTemplateEditor` / Cat2BugMarkdown DSL 提炼变量、参数与用法，形成 cat2bug-report-dsl skills
- 在 `readme/development/skills/` 建立索引与分类目录；skills 供后续 AI 编写/审阅模版时加载
- 按国标对文档类型与信息项的规定，分类并编写一组**默认报告模版**（Markdown + 合法 DSL）；模版拆分/合并规则以标准为准（可一对一，也可在标准允许的组合/裁剪下合并或拆附录）
- apply 阶段：国标 OCR/skills 与 Cat2Bug 变量 skills **多 agent 并行**，汇合后再写默认模版

## Capabilities

### New Capabilities

- `test-standards-skills`: GB/T 38634 与 ISO/IEC/IEEE 29119（以第 3 部分文档为主）沉淀为 skills：概念/过程/文档类型/技术要点、shall/should 信息项、符合性与裁剪原则
- `cat2bug-report-dsl-skills`: Cat2Bug 报告模版 Markdown DSL 完整参考（变量命名空间、计划下标、表列、图片样式、卡片等）及编写约束
- `national-standard-default-templates`: 按国标分类的默认测试报告模版集合；模版边界遵循标准对信息项/文档的定义与裁剪规则，不强行「一类一文件」

### Modified Capabilities

- （无）现有 `openspec/specs/` 无报告模版国标相关能力需改需求级行为

## Impact

- 新增目录：`readme/development/skills/`（及 OCR 中间文本，若保留）
- 可能新增/更新默认模版资源（位置在 design 中确定：如系统内置模版、fixtures 或 readme 下示例）
- 依赖：本机 OCR 工具链（GB 为扫描件）；ISO 29119-3 可直接抽文本作对照
- 不改运行时报告渲染引擎 API；默认模版必须使用现有 DSL，避免引入未实现语法
- 与进行中的 `report-detail-three-pane-comments` 无直接冲突，但同属报告域，apply 时注意勿混改无关编辑器代码
