# Spec: Excel 导出与表格列同步

## Requirements

### REQ-1 数据导出列集

当请求带 `exportScope=data` 且 `exportColumns` 有效时，导出 Excel 仅包含 `visible=true` 的列，列顺序与 `exportColumns` 数组顺序一致。

### REQ-2 导入模版列集

当 `exportScope=importTemplate` 时，输出列为 `visible=true` 或属于模块必填集合的列；顺序与 `exportColumns` 一致。必填列表头样式（红色）保持不变。

### REQ-3 向后兼容

未传 `exportColumns` 时，行为与变更前相同。

### REQ-4 用例关联缺陷数

`defectProcessingCount` 可在数据导出中出现；不得出现在导入模版中。

### REQ-5 缺陷 Excel 视图

缺陷表格视图与 Excel 视图共用列缓存，导出使用同一套列配置。
