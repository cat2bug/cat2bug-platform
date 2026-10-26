## Why

工作台自定义页签拖拽改序已有规格与 API，但 Sortable `filter` 回调把事件对象当成了 DOM 节点，导致**全部拖动被取消**，用户无法改序。缺陷列表已有 `tab_sort` 与 `PUT /system/tabs/sort`，前端未接 Sortable，自定义页签顺序只能依赖创建时间。两边应对齐：可拖、可落库、刷新后保持。

## What Changes

- **修复**工作台页签 Sortable：`filter(evt, target)` 正确使用 `target`；从「更多」菜单启动的指针不进入拖动；同组内拖序与 `PUT .../tabs/sort` 行为不变（不跨共享/私有；工作台与加号仍不可拖）。
- **新增**缺陷列表自定义页签拖拽改序：对齐工作台的 Sortable 模式（forceFallback、拖完还原 DOM 再改数据）；「全部」「已删除」、加号不可拖；落库走已有 `updateSortOfTabs`；刷新后按 `tab_sort` 显示。
- 不放宽 ADR-021（不跨可见性组、不拖动固定「工作台」）。

## Capabilities

### New Capabilities

- （无）

### Modified Capabilities

- `defect-list-tabs`：增加自定义页签拖拽改序与服务端持久化要求
- `workbench-custom-tabs`：无需求变更（仅修复实现，使既有「同组拖拽」重新可用）；可选在 delta 中补「更多菜单不启动拖动」场景以锁回归

## Impact

- 前端：`views/system/dashboard/index.vue`（Sortable filter）；`views/system/defect/index.vue`（绑 Sortable + `updateSortOfTabs`）
- API：复用既有 sort 接口，预计无后端契约变更
- 文档：`docs/04-UI规范/组件与交互约定.md` 可补页签拖序一句；ADR-021 不改
