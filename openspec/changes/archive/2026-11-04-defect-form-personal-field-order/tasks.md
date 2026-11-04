## 1. 表单排序核心

- [x] 1.1 改 `resolveOrderedDefectFormFields`：有个人 field-list 缓存时用 `mergeDefectTableColumnPrefs` 序；否则回退团队 `orderedEnabledFieldKeys`；显隐仍用团队 `isBuiltinVisible`
- [x] 1.2 更新 `defect-form-field-order.spec.js`：覆盖个人序优先、无缓存回退团队、列表 visible=false 仍出现在表单

## 2. 打开时重算

- [x] 2.1 `DefectFormOrderedFields` 暴露/完善重建入口；AddDefect `open`、EditDefectDialog `open` 时调用，保证读到最新个人序

## 3. 文档

- [x] 3.1 回写 `docs/04-UI规范/组件与交互约定.md` 表单列序口径；修正踩坑记录「表单只跟团队」；`docs/08-版本记录/2026-11.md` 一行；无新 ADR
