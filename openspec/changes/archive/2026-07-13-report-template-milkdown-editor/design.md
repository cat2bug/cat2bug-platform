## Context

报告模版编辑当前由 `Cat2BugMarkdown` 实现：左侧 `md-editor-v3`（CodeMirror 源码编辑），右侧 `markdown-it` 实时预览，顶部 `MarkdownTools` 工具栏插入 DSL 片段。模版正文存入 `sys_report_template.template_content`（纯文本）。

**Cat2Bug DSL 使用概况（代码分析 + 产品设计推断）：**

| 构造 | 频率 | 复杂度 |
|------|------|--------|
| 标准 Markdown | 极高 | 低（Milkdown 内置） |
| `${api.project.*}` | 极高 | 低（inline chip） |
| `${api.plan.*[n]}` | 高 | 中（30 字段 × 计划索引） |
| `$table{api.defect.list}` | 高 | 中（列配置 UI） |
| `$card{api.case.list}` | 中 | 高（多行表格 + 步骤子表渲染） |
| `$image{...}` | 中 | 低 |
| 计划统计大表 | 中 | 高（超宽原生 MD 表 + 大量 plan 变量） |
| multimd-table 合并单元格 | 低 | 高 |

渲染/导出链路（`ViewReport.vue`、Word/PDF）依赖同一套 `markdown-it` 插件（`TablePlugin`、`VariablePlugin` 等），**不在本次替换范围内**。

前端已升级至 Vue 3 + Vite + Element Plus（`vue3-vite-pinia-migration` 已完成），具备引入 Milkdown 生态条件。

## Goals / Non-Goals

**Goals:**

- 用 Milkdown 提供 WYSIWYG 模版编辑，降低 DSL 手写门槛
- **存储兼容**：`template_content` 仍为 Markdown + Cat2Bug DSL 纯文本
- **渲染兼容**：`markdown-it` 预览/导出零改动或最小改动
- Phase 0 交付缺陷表格可视化编辑（最高业务价值）
- DSL parse/serialize 有 unit test 门禁；模版编辑有 e2e 覆盖

**Non-Goals:**

- 替换 `ViewReport.vue` 渲染引擎
- 修改后端 `/markdown/{projectId}` API 或数据库 schema
- 一次性删除 `Cat2BugMarkdown`（渐进迁移 + fallback）
- 在 Milkdown 内复刻 `$card{api.case.list}` 的完整渲染逻辑（编辑态仅需块占位 + 配置；真实渲染仍走 `CaseCardPlugin`）
- 首期支持 `api.member.*`（工具栏已注释，使用极少）

## Decisions

### D1: 存储格式不变 —「DSL 为真相源」

**选择：** Milkdown 内部 ProseMirror doc ↔ Cat2Bug DSL 字符串双向转换；持久化始终写 DSL 文本。

**理由：** 已有模版无需迁移；`ViewReport`/导出/项目备份零破坏；可渐进上线。

**备选：** 存 ProseMirror JSON — 需全量迁移脚本，破坏导出格式，否决。

### D2: 渲染分层 — 编辑用 Milkdown，预览/导出用 markdown-it

**选择：** 编辑区 Milkdown + 自定义 node views；预览 pane 继续 `markdown-it` + 现有 plugins（与 `ViewReport` 同源配置）。

**理由：** `CaseCardPlugin`/`TablePlugin` 等渲染逻辑成熟；避免在 Milkdown 重复实现数据绑定与 i18n 表头。

**备选：** Milkdown 统一渲染 — 工作量大，导出链也需重写，否决。

### D3: 自定义节点策略 — `$node` + Vue `NodeView`

**选择：** 为每种 DSL 块注册 Milkdown schema node + Vue 组件 node view：

```
cat2bugVariable      → ${api.project.projectName}
cat2bugPlanVariable  → ${api.plan.defectCount[2]}
cat2bugDefectTable   → $table{api.defect.list[...]}
cat2bugCaseCard      → $card{api.case.list}
cat2bugDynamicImage  → $image[...]{...}
cat2bugRawBlock      → 无法建模的 DSL/MD 片段（只读或源码模式）
```

**序列化：** 每个 node 的 `toMarkdown()` 输出精确 DSL 字符串；`parseMarkdown()` 用与 `TablePlugin.js` 等价的正则/规则解析。

**理由：** Milkdown 官方推荐自定义 block 模式；Vue 3 下 `@milkdown/vue` 提供 node view 集成。

### D4: Phase 0 spike 范围 — 缺陷表 + 基础变量

**选择：** 首期只实现 `cat2bugVariable` + `cat2bugDefectTable` + 标准 GFM；其余 DSL 走 `cat2bugRawBlock` 透传。

**验收：** 合成样例模版（项目信息 + 缺陷表）open → edit columns → save → preview 与旧编辑器输出一致。

**理由：** 缺陷表是文档明确推荐、工具栏默认、用户感知最强的块。

### D5: 工具栏迁移 — 复用 `MarkdownTools.js` 数据结构

**选择：** 保留 `MarkdownTools.js` 菜单定义，将 `content` 模板映射为 Milkdown `insert` 命令；计划动态菜单（`addDynamicToolMenu`）逻辑抽到 composable `useReportTemplateTools(projectId)`。

**理由：** 减少 UX 回归；i18n 与插入内容单点维护。

### D6: 依赖选型

**选择：**

```json
"@milkdown/core": "^7.x",
"@milkdown/ctx": "^7.x",
"@milkdown/preset-commonmark": "^7.x",
"@milkdown/preset-gfm": "^7.x",
"@milkdown/vue": "^7.x",
"@milkdown/plugin-listener": "^7.x"
```

**理由：** Milkdown 7.x 与 Vue 3 官方集成；GFM 覆盖表格/删除线等。

**不移除：** `md-editor-v3`、`markdown-it` 直至 fallback 删除。

### D7: 组件布局

**选择：** 新建 `src/components/ReportTemplateEditor/`：

```
ReportTemplateEditor.vue      # 外壳：工具栏 + multipane + v-model
├── MilkdownEditor.vue        # Milkdown 实例
├── PreviewPane.vue           # 复用 MarkdownRenderer
├── toolbar/                  # 从 MarkdownTools 映射
├── nodes/                    # Vue node views
└── dsl/                      # parse.ts / serialize.ts / grammar.ts
```

`EditReportTemplate.vue` 将 `<cat2-bug-markdown>` 替换为 `<report-template-editor>`。

### D8: Fallback 策略

**选择：** 模版页提供「源码模式」切换，回退到现有 `MdEditorPane`；检测无法安全解析的 doc 时默认源码模式并提示。

**理由：** 计划统计大表、multimd 合并表短期难以 WYSIWYG；保证老模版可编辑。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| DSL round-trip 丢精度（空格、列顺序、中文别名） | golden-file unit tests；serialize 规则与 `TablePlugin` 正则对齐 |
| `$card{api.case.list}` 编辑体验复杂 | P1 再做；P0 用 raw block 透传 |
| 计划统计大表破坏 | raw block / 源码 fallback；不强制 WYSIWYG |
| Milkdown bundle 增大 | 仅模版编辑页 lazy import；`defineAsyncComponent` |
| 预览与 Milkdown 所见不一致 | 预览 pane 强制走 `markdown-it`（D2），不以 Milkdown 预览为准 |
| 多计划索引 UI 繁琐 | plan chip 带计划选择器；工具栏保留按 plan 插入 |

## Migration Plan

1. **Phase 0（1–2 周）**：`dsl/` parse/serialize + defect table node + 变量 chip；feature flag `reportTemplate.milkdown=true`；e2e + unit
2. **Phase 1（1–2 周）**：case card、image、plan variable chips；工具栏全量迁移
3. **Phase 2（按需）**：计划统计表模板插入优化；multimd 只读块；评估移除 `Cat2BugMarkdown`
4. **Rollback**：关闭 feature flag 即回退 CodeMirror 编辑器；`template_content` 格式未变，无需 DB 回滚

## Open Questions

- [ ] 是否在 Phase 0 即默认启用 Milkdown，还是 opt-in 一段时间？
- [ ] 缺陷表列配置 UI：Popover 还是侧栏抽屉？
- [ ] 是否需要对生产 DB 跑 DSL 使用统计脚本，调整 P1 优先级？（仓库内无样例模版）
- [ ] `$card{api.defect.list}` 是否在 P1 暴露工具栏入口？（解析器支持但当前无入口）
