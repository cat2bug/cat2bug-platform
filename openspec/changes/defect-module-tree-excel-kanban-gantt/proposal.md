## Why

缺陷「表格」视图已有左侧交付物树（`multipane` + `TreeModule`）做 `moduleId` 过滤，收起后表头首列角标可再展开，且 ⌘/Ctrl+D 可切换。Excel、看板没有对等侧栏，过滤体验不一致。甘特已有「按交付物」分组，**不**再加独立交付物侧栏。

## What Changes

- Excel / 看板增加与表格同语义的**真侧栏**交付物树：点节点写入父页共用 `queryParams.moduleId` 并刷新当前视图数据（看板为**纯过滤**，不按交付物重排卡片）。
- **甘特不设**独立交付物列表/侧栏（继续用既有分组 tabs）。
- 展开态：侧栏标题「交付物」右侧提供**隐藏**按钮（复用 `TreeModule` 的 `showSidebarToggle`）。
- 收起态入口按模式：
  - Excel：sheet 左上角 `th.first-col` 角标；
  - 看板：最左窄竖条，顶上「显示」图标（泳道壳视觉，非状态列）。
- 侧栏展开/收起与宽度：**Excel / 看板各自本地缓存**，不与表格的 `defect_tree_module_visible` / width key 共用。
- ⌘/Ctrl+D：在表格、Excel、看板切换当前视图侧栏显隐（甘特无操作）。
- 不改后端 API；不推翻已有 ADR。

## Capabilities

### New Capabilities

- `defect-module-tree-sidebar`: 缺陷 Excel / 看板左侧交付物树侧栏、收起角标/竖条、分视图可见性缓存，以及 ⌘D 在表格/Excel/看板切换。

### Modified Capabilities

- （无）既有 `defect-list-kanban` 未禁止侧栏；行为增量由新 capability 覆盖。表格既有行为保持，仅扩展快捷键作用域。

## Impact

- 前端：`views/system/defect/list/excel.vue`、`kanban.vue`；父页 `defect/index.vue`（⌘D）；可复用 `TreeModule`、`vue-multipane`、表格侧栏交互模式。`gantt.vue` 不改交付物侧栏。
- 文档：回写 `docs/04-UI规范/组件与交互约定.md`；`docs/08-版本记录` 当月一行。无新 ADR。
- 后端 / API：无变更。
