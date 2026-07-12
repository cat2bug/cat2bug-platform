## 1. Phase 0 — Spike & DSL Foundation

- [x] 1.1 创建 git worktree 分支 `feature/report-template-milkdown-editor`
- [x] 1.2 安装 Milkdown 7.x 依赖（`@milkdown/core`, `preset-commonmark`, `preset-gfm`, `vue`, `plugin-listener`）
- [x] 1.3 新建 `src/components/ReportTemplateEditor/dsl/grammar.ts`：从 `TablePlugin.js` / `VariablePlugin.js` 提取 DSL 正则与常量（缺陷 14 列、用例 10 列、plan 字段列表）
- [x] 1.4 实现 `dsl/parse.ts`：Markdown 字符串 → 结构化 AST（含 raw block fallback）
- [x] 1.5 实现 `dsl/serialize.ts`：AST → Markdown 字符串（字节级 round-trip 测试用）
- [x] 1.6 编写 unit tests：`tests/unit/report-template/dsl-roundtrip.spec.js`（项目变量、plan 变量、缺陷表默认列、列别名、case card、image）
- [x] 1.7 Spike：`MilkdownEditor.vue` 最小集成（标题 + 段落 + 保存 emit）
- [x] 1.8 实现 `cat2bugVariable` node + chip NodeView（`${api.project.*}`, `${api.case.total}`）
- [x] 1.9 实现 `cat2bugDefectTable` node + 列配置 Popover NodeView
- [x] 1.10 实现 `cat2bugRawBlock` node：未识别 DSL 原文透传
- [x] 1.11 ★ Milestone：合成样例模版 open → 改缺陷表列 → serialize → 与 golden DSL 文件 diff 为零

## 2. Phase 0 — Editor Shell & Integration

- [x] 2.1 实现 `ReportTemplateEditor.vue`：multipane 分栏 + v-model(`content`) + 工具栏槽位
- [x] 2.2 实现 `PreviewPane.vue`：复用 `MarkdownRenderer` + 现有 Cat2Bug plugins 配置（与 `ViewReport` 对齐）
- [x] 2.3 实现预览开关（等效 `mk-look` 工具）
- [x] 2.4 从 `MarkdownTools.js` 抽取 `useReportTemplateTools` composable（静态菜单部分）
- [x] 2.5 工具栏：插入项目变量、插入缺陷表（映射 Phase 0 节点）
- [x] 2.6 `EditReportTemplate.vue` 接入 `<report-template-editor>`，feature flag `VITE_REPORT_TEMPLATE_MILKDOWN` 控制
- [x] 2.7 保留「源码模式」fallback：切换至现有 `MdEditorPane`，共享同一 `template_content`
- [x] 2.8 样式：对齐 `cat2bug.scss` / `theme-dark.scss` 下编辑器与预览区主题
- [x] 2.9 编写 Playwright e2e：`e2e/report/template-edit.spec.js`（打开模版页 → 插入缺陷表 → 保存 → 预览区见表格头）
- [x] 2.10 ★ Milestone：Phase 0 验收——新编辑器可完成「项目信息 + 缺陷清单」模版编辑，保存后 `ViewReport` 渲染一致
  - **标准 DSL（E4 对比）**：`cat2bug-platform-ui/tests/fixtures/report-template/phase0-e4-viewreport-golden.md`
  - **验收记录**：E4 旧/新编辑器对比通过（项目信息 + 缺陷清单渲染一致，无控制台报错，round-trip 测试通过）

## 3. Phase 1 — Extended DSL Nodes & Toolbar Parity

- [x] 3.1 实现 `cat2bugPlanVariable` node + 计划索引选择器 NodeView
- [x] 3.2 实现 `cat2bugCaseCard` node（块占位 + 只读摘要）
- [x] 3.3 实现 `cat2bugDynamicImage` node + style 配置 UI
- [x] 3.4 迁移 `addDynamicToolMenu` 逻辑至 `useReportTemplateTools`（按项目计划动态生成菜单）
- [x] 3.5 工具栏补齐：用例总数、用例卡片、项目图片、计划变量、计划统计大表插入
- [x] 3.6 扩展 unit tests：plan 30 字段抽样、image style 变体、case card
- [x] 3.7 扩展 e2e：插入 plan 变量 + case card 保存预览
- [x] 3.8 ★ Milestone：工具栏能力与 `Cat2BugMarkdown` 对齐（除已注释的 member 表）

## 4. Phase 2 — Edge Cases & Cleanup

- [x] 4.1 计划统计大表：插入模板优化（保持 `${api.plan.*[n]}` 变量完整性）
- [x] 4.2 multimd-table 合并单元格：raw block 只读展示 + 源码编辑提示
- [x] 4.3 对齐语法（`:--`/`-:-`/`--:`）与扩展 MD（`++/==/~~/^/~`）工具栏插入验证
- [x] 4.4 Milkdown 编辑页 lazy import（`defineAsyncComponent`）验证 bundle 影响
- [x] 4.5 可选：DB 扫描脚本统计真实模版 DSL 使用频率，调整优先级
- [x] 4.6 默认启用 Milkdown（移除 feature flag 或改为默认 true）
- [x] 4.7 评估并移除 `Cat2BugMarkdown` 编辑职责（保留渲染工具函数若仍被引用）
- [x] 4.8 更新用户文档 `template-create.md`：Milkdown 可视化编辑说明
- [x] 4.9 ★ Milestone：回归 `ViewReport` Word/PDF 导出 + 项目 `report_template.json` 导入导出

## 5. Documentation & Archive

- [x] 5.1 在 `readme/design-system/` 或模版文档补充 DSL ↔ Milkdown 节点映射表
- [x] 5.2 运行 `/opsx:archive` 归档本 change，同步 specs 至 `openspec/specs/`
