## Context

报告模版编辑器（`ReportTemplateEditor` + Milkdown）中，`cat2bug_variable` / `cat2bug_plan_variable` 目前由 `createLiveChipView` 渲染为始终可见的 chip；解析成功后虽会去掉边框，但仍与生产侧 `VariablePlugin` / `PlanVariablePlugin`（直接 push 普通 `text`）的「行内纯文本」体验不一致。块节点（缺陷表等）已有左上角图标 + header 的视觉语言，但变量不应沿用始终可见的块 chrome。

本变更只改可视化 NodeView 与工具栏全局态，不改 DSL 语法、序列化或 ViewReport 渲染管线。

## Goals / Non-Goals

**Goals:**

- 「变量」= 内置 `${…}` 与 `$xxx{…}`（`$table` / `$card` / `$image`）及计划 `${api.plan…[n]}`
- 默认未悬停：与生产 Markdown 一致（`${}` 为行内纯文本；`$table`/`$card`/`$image` 为预览内容，无编辑 chrome）
- 悬停：虚线边框 + 左上角两态按钮 + 标题行
- 两态：内容态 = 解析值/预览；变量态 = 原始 token
- 工具栏 checkbox：全部内容态 ↔ 全部变量态；默认全部内容态

**Non-Goals:**

- 不把计划统计大表等 raw 保留块当作单一变量单元；视觉上按 Markdown 表格渲染，表内 `${}` 随工具栏展开/折起切换内容与 token
- 不把展开态写入模版 DSL 或后端
- 不改源码模式编辑体验
- 不改最终报告（ViewReport）渲染结果

## Decisions

### D1: 默认行内纯文本，悬停再挂编辑 chrome

- **选择**：NodeView 根保持可嵌在段落中的行内宿主；未悬停时仅渲染当前态文案（无边框/按钮/标题）；悬停时再挂 chrome（虚线框、两态按钮、标题）。
- **理由**：生产插件输出普通 `text`；「默认显示要和生产一样」是硬约束。真正的 block 迷你块会打断段落，无法满足对齐。
- **备选**：始终可见迷你块 → 拒绝（破坏生产对齐）。

### D2: 左上角两态按钮取代装饰图标与右侧箭头

- **选择**：悬停 header 左侧为唯一切换控件（内容 ↔ 变量），无独立装饰 icon、无右侧 chevron。
- **理由**：用户明确要求「折叠图标放到左上角图标位，去掉原图标，变成两态图标」。
- **备选**：右侧 ▶/▼ + 左侧装饰 icon → 拒绝。

### D3: 变量态正文 = raw token；标题另起一行

- **选择**：变量态正文使用 `attrs.raw`（如 `${api.project.projectName}`）；悬停标题优先工具栏/菜单中文标签，缺失则退回路径短名（如 `projectName`）或 `path`。
- **理由**：用户选定「变量名用原始 token」指正文；标题用于快速识别业务含义，不替代 token。
- **备选**：标题也显示 raw → 与正文重复，信息密度差。

### D4: 全局展开态为编辑器会话态

- **选择**：在编辑器壳（`ReportTemplateEditor` 或共享 composable/module）维护 `variablesExpanded: boolean`（true = 内容态），经订阅通知所有变量 NodeView；单节点点击两态按钮可本地覆盖，工具栏全开/全折重置全部为全局值。
- **理由**：无需持久化；刷新回到默认全展开符合预期。
- **备选**：写入文档 attrs → 污染 DSL、与生产无关，拒绝。

### D5: 工具栏使用现有 `type: 'check'`

- **选择**：在 `loadReportToolbarMenus` / 文件命令区增加 check 工具；`ReportTemplateToolbar.onToolSelect` 接通 check（今日为空操作），切换 `variablesExpanded` 并更新图标/文案。
- **理由**：与历史 `Cat2BugMarkdown` 编辑/预览切换一致；比在 view-mode segment 旁再加按钮更统一。
- **备选**：独立按钮在 `__view-tools` → 可用，但与既有 ToolsMenu 模式不一致。

### D6: 继续复用预览解析管线

- **选择**：内容态仍走 `resolvePreviewVariable` / `resolvePreviewPlanVariable` + `report-preview-data`（loading → `…`，empty/error → `—`）。
- **理由**：与现网实时预览数据源一致，避免第二套解析。

## Risks / Trade-offs

- **[Risk] 悬停 chrome 与行内排版冲突（绝对定位遮挡邻行）** → Mitigation：悬停框用 `::before` / absolute overlay，不改 display/padding/border，正文位置不变；限制 z-index，标题单行省略。
- **[Risk] 全局态与单节点本地覆盖不同步** → Mitigation：明确规则——工具栏操作重置全部；单点只改当前节点直至下次全局操作。
- **[Risk] 标题标签映射不全** → Mitigation：建 path→i18n 映射表覆盖工具栏常见插入项；未命中用 path 末段，不阻塞渲染。
- **[Risk] 计划统计大表被误做成单一折叠单元** → Mitigation：明确排除 raw 保留块；仅独立 `${}` / `$table` / `$card` / `$image` NodeView 参与两态。
- **[Risk] `$card` 内容预览与生产 CaseCardPlugin 完整步骤表不一致** → Mitigation：先用 id+名称紧凑列表保证可识别；后续可按需加深预览保真度。

## Migration Plan

- 纯前端 UI；无需数据迁移。
- 上线后旧模版自动获得新 NodeView；保存内容不变。
- 回滚：还原 NodeView/工具栏提交即可。

## Open Questions

- 两态图标具体素材（SVG / svg-icon 名）实现时选定，语义固定为「内容 ↔ 变量」。
- 悬停离开是否立即收起 chrome：建议短 delay（~100–150ms）以免移向按钮时闪烁；实现时按手感微调。
