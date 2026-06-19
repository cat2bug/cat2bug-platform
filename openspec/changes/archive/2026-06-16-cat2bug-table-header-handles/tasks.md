## 1. 列缓存与合并

- [x] 1.1 在 `mergeDefectTableColumnPrefs` 中合并并应用缓存条目 `width`（缺省回退 defaults / `width_xx`）
- [x] 1.2 更新 `syncNewDefectTableColumnsIntoFieldListCache`：新列写入 field-list 时 `width` 可选
- [x] 1.3 确认 `setColumns` / `saveShowColumns` 持久化完整列对象（含 `width`），debounce 调宽写入

## 2. Cat2BugTable 表头模板与样式

- [x] 2.1 表头 DOM：`col-handle-drag`（pin 左）→ pin → `header-title`；`col-handle-resize` 绝对定位右缘
- [x] 2.2 拖序占位固定宽 + 未 hover 透明；调宽竖条 hover 显隐；`th` 相对定位
- [x] 2.3 新增拖序/调宽图标或 CSS（圆角矩形六点、圆角竖条）；`cat2bug.scss` + `theme-dark.scss`
- [x] 2.4 排除操作列、prepend 列、`pinFixedToggle === false` 列的手柄渲染

## 3. 拖序（Sortable）

- [x] 3.1 Sortable `handle: '.col-handle-drag'`，调整 `filter`（不再依赖整列 `.table-header`）
- [x] 3.2 主表与左固定表 `thead tr` 双区 `group` 行为回归；拖序后 `reorderColumns` / 缓存写入
- [x] 3.3 验证 pin 单击一次即切换固定，与拖序手柄无冲突

## 4. 调宽

- [x] 4.1 实现 `col-handle-resize` mousedown → document mousemove/mouseup，clamp `width >= 60`
- [x] 4.2 `el-table-column` 绑定用户 `width` + `min-width` 下限；更新 `columnMinWidth` / `columnWidth` 逻辑
- [x] 4.3 调宽后 `doLayout`、`syncFixedDataRowHeights`、`updateCustomHorizontalScrollbar`；操作列自动宽不受影响

## 5. 布局稳定性验收

- [x] 5.1 对比未 hover / hover 截图或测量：pin、标题、排序箭头水平位置一致
- [x] 5.2 左固定双表头下手柄与占位规则一致

## 6. 全站回归

- [x] 6.1 缺陷列表：拖序、调宽、pin、显隐、暗色主题
- [x] 6.2 用例 / 计划 / 文档 / 报告列表 smoke
- [x] 6.3 计划抽屉 `CaseList` / `DefectList` smoke
- [x] 6.4 必填列（缺陷名称、处理人）红色标题与手柄共存

## 7. 文档（可选）

- [x] 7.1 在 `readme/` 或变更目录补充简短手工测试步骤（若团队需要）
