## Context

缺陷表格视图（`list/table.vue`）已用 `vue-multipane` + `TreeModule` 实现左侧交付物树。Excel / 看板需对等过滤侧栏。甘特已有「按交付物」分组，产品确认**不需要**再挂独立交付物列表。

## Goals / Non-Goals

**Goals:**

- Excel / 看板具备与表格同语义的真侧栏交付物过滤。
- 收起入口：Excel 角标、看板竖条；展开态标题右侧隐藏。
- 分视图本地缓存可见性与宽度；`moduleId` 仍走父页共用 `queryParams`。
- ⌘/Ctrl+D 覆盖表格 / Excel / 看板。

**Non-Goals:**

- 甘特独立交付物侧栏。
- 看板按交付物重排/多维泳道。
- 改甘特「按交付物分组」逻辑或 `plan-node` API。
- 新建独立树组件或后端接口。

## Decisions

### D1：复用 TreeModule + multipane（Excel / 看板）

各视图自挂壳；过滤写父 `moduleId` 后走既有 `search`。

### D2：收起入口

| 视图 | 收起 UI |
|------|---------|
| Excel | `th.first-col` 区域展开角标 |
| 看板 | 最左窄竖条 + 顶上显示图标 |

### D3：缓存 key

- Excel：`defect_excel_tree_module_visible` / `defect_excel_tree_module_width`
- 看板：`defect_kanban_tree_module_visible` / `defect_kanban_tree_module_width`

### D4：快捷键

`shortcutToggleModuleTree` 仅对 `DefectTable` / `DefectExcel` / `DefectKanban` 生效。

### D5：文档

回写 `docs/04`；无新 ADR。

## Risks / Trade-offs

- [Risk] Excel `first-col` 点击与行选冲突 → `stopPropagation` + 绝对定位角标。
- [Risk] 看板加最左栏后泳道宽度需重算 → 交付物栏与状态泳道 resizer 隔离。

## Migration Plan

纯前端；回滚还原 excel/kanban/`index.vue` 即可。

## Open Questions

无。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`
- `docs/00-项目总览.md`
- 拟新增 ADR：无
