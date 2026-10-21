## 1. 列配置

- [x] 1.1 在 `plan-table-options.js` 为 `plan.name` 增加 `required: true`
- [x] 1.2 在 `module/index.vue` 的 `MODULE_TABLE_COLUMNS` 为 `module.name` 增加 `required: true`
- [x] 1.3 在 `document-table-options.js` 为 `doc.name` 增加 `required: true`

## 2. 验收

- [x] 2.1 Playwright：计划「测试计划名称」、交付物「交付物名称」、文档「文档/文件夹名称」均为 `rgb(245, 108, 108)` 且无星号
- [x] 2.2 写入 `plan-table-field-list` 宽 240 后刷新：名称列宽 240 且仍红；用例页仍为名称/交付物/预期三列必填红

## 3. 回写长期知识

- [x] 3.1 已在 `docs/04-UI规范/组件与交互约定.md`「表格：Cat2BugTable」补「必填列表头」一句；无新 ADR / 踩坑
