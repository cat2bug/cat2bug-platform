# ADR-019 可视化 Markdown 统一用 Tiptap，连续 A4 纸面

- 日期：2026-10-19
- 状态：已采纳

## 背景

产品里需要一种可复用的可视化 Markdown 编辑：所见即所得、顶栏格式工具、可插图片（含粘贴与缩放）、以后还能挂自定义 Vue 节点和自定义 Markdown 语法。旧报告模版曾用 Milkdown（见 [[ADR-009-报告模版编辑器用Milkdown且DSL为真相源]]），报告模块已下线（[[ADR-018-移除报告模块且不保留历史数据]]）。缺陷描述等仍可用源码态 `Cat2BugMarkdown`；新建的可视化编辑面要统一选型，并约定「屏幕排版与打印观感接近」时怎么做。

## 候选方案

### 方案 A：md-editor-v3 / Vditor 等封装编辑器

优点：开箱即用，工具条现成。

缺点：自定义 Vue 节点与自定义 Markdown 往返弱；难做成平台统一扩展面。

### 方案 B：开源 Tiptap + 自研连续纸面（本决定）

优点：ProseMirror 内核，Vue 节点视图成熟；开源核足够日常文档；工具条可复用既有 `MarkdownTools` / `ToolsMenu`；纸面用 CSS 定 A4 宽与页边距，正文连续滚动，不引入付费分页。

缺点：没有「一页一页」的自动分页；浏览器打印由 `@page` 再分页，换页位置与屏幕上「一整条纸」不完全一一对应，但行宽与边距可对齐。

### 方案 C：Tiptap 付费 Pages 扩展

优点：真正的纸张分页、页眉页脚、与 Conversion 导出对齐。

缺点：付费与 Beta；要换配套文档骨架与表格；不接浏览器打印对话框；过高且不可切分的块会拖死布局。当前产品明确不要分页效果，成本过高。

## 最终决定

1. **可视化 Markdown 统一组件**：`cat2bug-platform-ui/src/components/Cat2BugEditor`（Tiptap 3 + `@tiptap/markdown`）。存盘真相源仍是 Markdown 字符串。
2. **顶栏**：复用 `MarkdownTools.js` + `ToolsMenu`；默认不带项目 / 用例 / 缺陷等业务插入项（`includeBusinessTools`）。
3. **纸面**：默认连续 A4 样式——内容区宽 210mm、页边距上/下约 25mm、左/右约 20mm，浅底上的「纸」居中，内容再长也只加长这一张纸，**不做分页断页、不做付费 Pages**。窄屏时纸宽不超过容器。
4. **打印**：组件提供与纸面一致的 `@page { size: A4 }` 与页边距约定，使打印行宽接近编辑；不承诺页间断点与屏幕一致。
5. **导入导出 PDF/DOCX**：不绑死在开源核上；需要时走旁路转换层（含可选 Tiptap Conversion），与是否上 Pages 无关。
6. **缺陷等源码编辑**：暂可继续用 `Cat2BugMarkdown`；迁到 `Cat2BugEditor` 时另开变更，不复活报告模块。

## 原因

- 扩展自定义组件与 Markdown 语法时，Tiptap 路径最短。
- 产品要的是「按 A4 规格排版、显示与打印基本一样」，不是 Word 式多页分页。
- 连续纸面用开源核 + CSS 即可落地，避免付费分页与配套 schema 更换。

## 副作用

- 超大图片等会把「纸」拉得很长，需依赖编辑区内滚动；图片须可缩放，粘贴走统一上传。
- 有尺寸的图片在 Markdown 里写成带 `width`/`height` 的块级 HTML（`<div><img …></div>`），标准 `![]()` 带不走尺寸。
- 弹框等窄场景可用 `paper=false` 退回铺满容器，避免 A4 宽把对话框撑坏。
- 交互与样式细则见 [[组件与交互约定]] 中「可视化 Markdown：Cat2BugEditor」。
