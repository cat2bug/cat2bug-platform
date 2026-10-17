# ADR-009 报告模版编辑器用 Milkdown，DSL 为真相源

记录报告模版改为所见即所得编辑时，存储格式与渲染分层的取舍。

- 日期：2026-07-13
- 状态：被 [[ADR-018-移除报告模块且不保留历史数据]] 取代

## 背景

报告模版用 Cat2Bug DSL（Markdown + 变量、缺陷表等自定义块）书写，原为源码编辑，门槛高。前端已迁 Vue 3（[[ADR-005-前端迁移Vue3-Vite-ElementPlus-Pinia]]），希望引入所见即所得编辑。依据：`openspec/changes/archive/2026-07-13-report-template-milkdown-editor/design.md`。

## 候选方案

### 方案 A：Milkdown 内部 ProseMirror doc ↔ DSL 双向转换，持久化始终写 DSL

优点：已有模版无需迁移；报告查看、导出、项目备份零破坏；可渐进上线。

缺点：每种 DSL 块都要写 schema node 与转换。

### 方案 B：直接存 ProseMirror JSON

缺点：需全量迁移脚本，破坏导出格式，否决。

### 渲染：Milkdown 统一渲染 vs 编辑/预览分层

统一渲染工作量大、导出链也要重写，否决。

## 最终决定

- 存储：DSL 文本是唯一真相源。
- 编辑区 Milkdown 7.x（`@milkdown/core`、`preset-commonmark`、`preset-gfm`、`@milkdown/vue` 等）+ 自定义 `$node` 与 Vue NodeView；预览/导出继续 `markdown-it` + 现有插件（与 `ViewReport` 同源）。
- 首期只实现变量与缺陷表块，其余 DSL 走 raw block 透传。
- 工具栏复用 `MarkdownTools.js` 数据结构。
- 提供「源码模式」回退到 `MdEditorPane`；无法安全解析时默认源码模式。

## 原因

兼容存量模版与导出链，降低回归风险；成熟的 markdown-it 插件不重复实现。

## 副作用

- 编辑与预览两套渲染，需保证一致；分屏编辑曾出现焦点与空行同步问题（提交 `795e9945`）。
- `md-editor-v3`、`markdown-it` 在 fallback 删除前不能移除，增加前端体积。
