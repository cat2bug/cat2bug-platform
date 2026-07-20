## Context

- 产品已有报告模版 Markdown DSL（见 `openspec/specs/report-template-dsl`）与 Milkdown 编辑器；变量族在 `ReportTemplateEditor/dsl/grammar.js` 与 Cat2BugMarkdown plugins。
- 用户提供 GB/T 38634.1–.4（扫描 PDF）与 ISO/IEC/IEEE 29119-3:2021（可抽取文本）。GB 与 29119 系列对应：概念/过程/文档/技术。
- 目标不是改渲染引擎，而是产出 **AI skills + 按国标合规的默认模版**。
- 约束：模版边界**跟从国标对信息项/文档的定义与裁剪（tailoring）规则**，禁止机械规定「标准里出现的每一种文档类型必须各有且仅有一份独立模版文件」。

## Goals / Non-Goals

**Goals:**

- OCR 读全 GB 四册，与 ISO 29119-3 对照，写入 `readme/development/skills/standards/`
- 完整沉淀 Cat2Bug DSL/变量 skills 至 `readme/development/skills/cat2bug-report-dsl/`
- 建立 taxonomy：国标文档/信息项 ↔ 推荐模版形态（独立 / 组合 / 附录 / 骨架）
- 产出一组默认可渲染模版（合法 DSL），覆盖国标要求的主要报告交付场景
- apply 时国标线与 DSL 线多 agent 并行

**Non-Goals:**

- 不实现国标符合性自动审计引擎或证书
- 不新增未实现的 DSL 语法；缺变量的章节用静态占位或标明「人工填写」
- 不强制把组织级方针/实践等长文政策文档全部做成动态模版（可仅提供结构骨架）
- 不在本变更中改动 Milkdown 编辑器核心行为（除非默认模版入库需要极小接线）

## Decisions

### D1 — 模版边界跟从国标，而非一类一文件

- **选择**：以 GB/T 38634.3 / ISO 29119-3 的信息项与符合性（full / tailored）为准。若标准将若干信息项定义为**同一文档**内的必选/可选章节，则对应**一份**模版；若标准定义为独立文档，则默认**分模版**；若标准允许裁剪合并，taxonomy 中写明「推荐独立」与「允许并入」两种形态。
- **替代**：强制 1:1 文件映射 —— 已否决（用户明确：不一定一类一模版，按国标）。
- **替代**：一个超级模版含全部类型 —— 已否决（违背文档类型边界与可读性）。

### D2 — Skills 落盘位置与形态

- **选择**：`readme/development/skills/`，含 `SKILL-INDEX.md`、`standards/`、`cat2bug-report-dsl/`、`report-templates/`；每份 skill 含简短 frontmatter（name/description）+ 可执行规则 + 示例。
- **替代**：仅放 `.cursor/skills/` —— 可选后续镜像；本变更以 readme/development 为源，便于仓库审阅。

### D3 — OCR 与标准源优先级

- **选择**：apply 先安装 OCR 工具链，GB 全文落到 `_sources/ocr/`（或同等路径）；skills 正文以 OCR+人工校对摘要为准，ISO 29119-3 作第 3 部分结构/shall 对照。扫描失败页标记 TODO。
- **替代**：仅用 ISO 英文 —— 可作临时，但不满足「国标」名义，仅作并行对照源。

### D3b — 分段读取（避免上下文爆炸）

国标 PDF/OCR 体量大（单册可达数十～上百页），**禁止**在写 skill / 写模版时把整本（或整册 OCR 全文）一次性塞进 agent 上下文。

- **选择**：分层切片 + 按需加载
  1. **物理切片**：OCR 输出按「册 → 章/条款 → 页码区间」拆成小文件，例如  
     `_sources/ocr/gb-t-38634-3/ch07-test-management/`、`…/p045-p052.md`，并维护每册 `manifest.json`（标题、起止页、对应 ISO 条款）。
  2. **逻辑切片**：skills **只保留结构化摘要**（信息项表、shall/should、裁剪规则、与模版形态映射）；正文细节用「路径 + 页码」指针回指 `_sources`，不整章粘贴进 skill。
  3. **读取协议**（写入 `SKILL-INDEX.md` / standards overview）：  
     - 先读索引/manifest → 再只打开当前任务相关的 1～N 个切片（建议单次合计控制在可消化篇幅，如数页～一章）；  
     - 多 agent 时按**册或章**分治，避免每个 agent 都加载全套 OCR；  
     - 需要核对原文时再追加打开相邻切片，禁止「先读完全部四本再开始写」。
  4. **ISO 对照**：29119-3 同样按章/信息项切片存放；与 GB.3 对照时两侧各取对应切片，而非两侧全文。
- **替代**：单文件巨量 OCR + 依赖模型长上下文 —— 已否决（易截断、贵、不可复现）。
- **替代**：不做全文 OCR、只靠目录臆造 —— 已否决（不符合「先 OCR 读全」；读全指**落盘完整**，不是**一次读入上下文完整**）。

### D4 — 默认模版交付形态

- **选择**：Markdown 文件（含 DSL）放在 `readme/development/skills/report-templates/29119-3/`；若产品需要入库，另增可选任务：导入为系统预置模版（design 允许分阶段，首期以 skills/仓库文件为准）。
- **替代**：直接写库 SQL seed —— 首期不做，降低耦合。

### D5 — Apply 并行策略

```
Agent-Standards: OCR GB（整册落盘）→ 按章切片/manifest
                 → 每次只读相关切片 → standards skills → taxonomy 初稿
Agent-DSL:       grammar/plugins → variables-reference 等 DSL skills
汇合 Agent-Templates: taxonomy 定稿 → 按需回读少量标准切片 → 默认模版
```

### D6 — 与现有 spec 关系

- **引用** `report-template-dsl`（不修改其需求）；默认模版 MUST 符合既有 token 语法。
- 本变更新增三条 capability specs，描述 skills/模版产物的验收行为。

## Risks / Trade-offs

- [GB OCR 质量差] → 保留原文页码引用；关键 shall 表以 ISO 29119-3 交叉验证；人工抽检目录与第 3 部分文档类型表
- [国标裁剪导致模版边界争议] → taxonomy 必须写清「标准依据 + 推荐形态 + 禁止事项」；有争议时以第 3 部分信息项定义为准
- [DSL 覆盖不足] → 模版中标注「静态占位」；不发明伪变量
- [skills 体积过大] → 标准 skill 用结构化摘要 + 指向 OCR 源，避免整本粘贴
- [上下文不够 / 截断] → 强制分段读取（D3b）：整册落盘、按章按页加载；SKILL-INDEX 写明读取协议
- [与 report-detail 变更并行] → 本变更原则上不改编辑器源码

## Migration Plan

1. 安装 OCR → 生成 `_sources/`
2. 并行写 standards + dsl skills
3. 定 taxonomy → 写默认模版
4. 用现有单测/手工预览验证模版 token 可解析
5. 回滚：删除 `readme/development/skills/` 下本变更新增文件即可

## Open Questions

- 默认模版是否需要在 apply 同期导入 `sys_report_template` 预置数据？（默认：否，仅仓库文件）
- OCR 语言包：`chi_sim` + `eng` 是否足够？（默认：是）
- 组织级「测试方针/实践」是否仅出骨架模版？（默认：是）
