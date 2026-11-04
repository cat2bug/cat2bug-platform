## 1. 合并逻辑：个人列序权威

- [x] 1.1 改 `mergeDefectTableColumnPrefs`：以缓存 key 序输出；新团队字段按 design「前驱后插入 / 无前驱则末尾」；剔已删 key；仍合并 visible/fixed/width
- [x] 1.2 更新 `defect-display-field.js` 注释与相关单测（若有）/ 补最小单测覆盖新字段插入与删除
- [x] 1.3 确认导出路径 `resolveDefectExportColumns` 等走同一 merge 后行为符合预期（个人序或显式说明）

## 2. 表格「显示字段」拖序

- [x] 2.1 `table.vue` 显示字段列表加拖柄 + Sortablejs（handle 与 checkbox 分离）
- [x] 2.2 `onEnd` 重排 → `setColumns` / 写缓存 → 与既有 `columns-change` / picker live columns 双向一致
- [x] 2.3 回归表头拖序仍更新 picker，且刷新后个人序保留；键盘 popover 导航不坏

## 3. Excel 共用缓存与拖序

- [x] 3.1 Excel 读写改为 `getDefectTableColumnCacheKey()`；旧裸键迁移到用户键
- [x] 3.2 Excel「显示字段」支持同等拖序，写回共享 field-list；切换表格视图序一致

## 4. 边界与文档

- [x] 4.1 确认新建/编辑表单与团队属性页仍只跟团队 layout / sortOrder
- [x] 4.2 手工：面板拖、表头拖、刷新、新字段落点、Excel↔表格、另一账号默认序
- [x] 4.3 回写长期知识：改 `docs/04-UI规范/组件与交互约定.md` 列序口径；`docs/08-版本记录/2026-10.md` 一行；无新 ADR / 无新踩坑则注明
