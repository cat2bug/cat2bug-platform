# Tasks

## 1. Cat2BugTable 组件

- [x] 1.1 表头模板：`col.required` 时为 `header-title` / `header-title-only` 增加 `header-title--required` class
- [x] 1.2 样式：`.header-title--required { color: #f56c6c; }`（两种表头结构均覆盖）

## 2. 列配置

- [x] 2.1 `case-table-options.js`：`case.name`、`module`、`expect` 设置 `required: true`
- [x] 2.2 `defect/list/table-options.js`：`type`、`defect.name`、`state`、`handle-by` 设置 `required: true`（`priority` 不设置）

## 3. 验证

- [x] 3.1 用例页表格模式：名称/交付物/预期表头红色，其它列默认色
- [x] 3.2 缺陷页表格模式：类型/名称/状态/处理人红色，等级等默认色
- [x] 3.3 调整列显隐与顺序后刷新，必填列红色仍正确
