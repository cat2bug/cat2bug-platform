## Why

缺陷的类型、优先级、状态在表格模式、Excel 模式、导入模版与新建/编辑弹窗之间的必填规则不一致：部分入口强制填写，部分入口已非必填，且空值缺少统一的默认值策略。用户希望降低录入摩擦，同时在保存时保证数据完整——类型默认 BUG、优先级默认「中」、状态默认「待处理」。

## What Changes

- 缺陷**类型**、**优先级**、**状态**在创建/导入时均改为**非必填**；空值在持久化前应用默认值（`BUG` / `middle` / `PROCESSING`）。
- **编辑**时：类型、优先级若被清空，保存时回落到上述默认值；**状态在编辑 UI 上不可清空**（下拉无 clearable，必须保留有效状态）。
- **表格模式**：`table-options` 去掉类型、状态的 `required` 表头标识（优先级已为非必填）。
- **Excel 模式**：`COLS` 去掉类型、优先级的 `required`；占位行创建与单元格保存前补默认；已有行编辑状态列不允许置空。
- **导入 Excel 模版与 `importDefect`**：模版必填集不再包含类型、状态；导入时空类型/状态/优先级写入默认而非报错。
- **导出**：数据导出行为不变；导入模版红头与必填列与新区规则一致。
- **AddDefect / HandleDefect 弹窗**：移除类型、优先级的表单必填校验；新建默认展示/提交可留空并由后端或提交前 normalize；HandleDefect 状态选择不可清空。
- **后端**：在 `insertSysDefect`、`updateSysDefect`、`importDefect`（含批量插入）前集中 `applyDefectDefaults`，避免各入口重复逻辑。

## Capabilities

### New Capabilities

- `defect-field-defaults`: 缺陷类型/优先级/状态的可选录入、默认值、创建与编辑时的 normalize 规则（含状态编辑不可清空）。

### Modified Capabilities

- `table-required-column-header`: 缺陷表格模式必填列集合变更——类型、状态不再标红必填。
- `excel-export-table-columns`: 缺陷导入模版 `DEFECT_TEMPLATE_REQUIRED` 与可见必填列规则变更。
- `excel-import-i18n`: 缺陷 Excel 导入空类型/状态时的校验与默认填充行为变更（与多语言标签解析配合）。

## Impact

- 前端：`table-options.js`、`defect/list/excel.vue`、`DefectImport.vue`、`AddDefect.vue`、`HandleDefect.vue`（及 `EditDefectDialog` 若共用规则）
- 后端：`SysDefectServiceImpl`、`ExcelColumnExportSupport.DEFECT_TEMPLATE_REQUIRED`、可选新增 `DefectDefaults` 工具类
- 测试：导入相关单测、缺陷默认值单元测试
- 文档：用户指南中缺陷必填字段说明（若存在）需后续同步
