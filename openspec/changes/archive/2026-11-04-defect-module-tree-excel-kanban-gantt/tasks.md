## 1. 共享接线约定

- [x] 1.1 对照 `list/table.vue` 整理侧栏模式：`TreeModule` + 显隐/宽度本地缓存 + `moduleTreeClickHandle` → 父 `queryParams.moduleId` + `toggleModuleTreeVisible` 对外方法
- [x] 1.2 确定 Excel / 看板各自的 visible/width 缓存 key（不覆盖 `defect_tree_module_*`）；甘特不设侧栏

## 2. Excel 侧栏

- [x] 2.1 `excel.vue` 外层接入 multipane + `TreeModule`（展开态标题旁隐藏）
- [x] 2.2 收起时在 `th.first-col` 放置展开角标；处理与 sticky/行选的点击冲突
- [x] 2.3 节点点击写 `moduleId` 并触发 Excel 列表刷新；暴露 `toggleModuleTreeVisible`

## 3. 看板侧栏

- [x] 3.1 `kanban.vue` 最左增加交付物泳道壳 + 内嵌 `TreeModule`（纯过滤，不改泳道分组）
- [x] 3.2 收起后保留窄竖条与顶上「显示」；拖宽与状态泳道 resizer 隔离
- [x] 3.3 过滤后刷新/重拉卡片；暴露 `toggleModuleTreeVisible`；独立缓存 key

## 4. 甘特

- [x] 4.1 确认甘特**不**接入独立交付物侧栏（保留既有「按交付物」分组）

## 5. 快捷键与父页

- [x] 5.1 `defect/index.vue` 的 `shortcutToggleModuleTree` 覆盖 Table / Excel / Kanban（不含 Gantt）
- [x] 5.2 手动验证：三视图 ⌘/Ctrl+D、分视图显隐缓存互不覆盖、`moduleId` 切视图仍跟随父查询

## 6. 回写长期知识

- [x] 6.1 更新 `docs/04-UI规范/组件与交互约定.md`（侧栏范围不含甘特）
- [x] 6.2 `docs/08-版本记录` 当月一行提及本变更；无新 ADR / 无可复用踩坑则注明无
