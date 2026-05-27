# 提案：表格必填列表头红色标识

## Why

用例、缺陷的 Excel 导入模版与导出已对必填列使用红色表头，但列表「表格模式」表头样式一致为默认色，用户难以在界面上区分必填列与可选列。需要在通用表格组件中统一支持必填列视觉标识，并与导入业务规则对齐。

## What Changes

- 在 `Cat2BugTable` 列配置中新增可选布尔属性 `required`
- 当 `required === true` 时，表头文字显示为红色（`#f56c6c`），**不**添加 `*` 前缀
- 用例列表 `case-table-options.js`：用例名称、交付物、预期 标记为必填
- 缺陷列表 `table-options.js`：类型、缺陷名称、状态、处理人 标记为必填
- 列显隐本地缓存结构不变；`mergeCachedColumns` 从默认列定义合并 `required`
- **不在本次范围**：缺陷 Excel 视图 `COLS` 等级必填修正、列选择器必填提示、与后端导出映射抽公共常量

## Capabilities

### New Capabilities

- `table-required-column-header`：Cat2BugTable 必填列表头红色渲染及用例/缺陷列配置

### Modified Capabilities

（无）

## Impact

- **前端**：`cat2bug-platform-ui/src/components/Cat2BugTable/index.vue`（模板与样式）
- **前端**：`case-table-options.js`、`defect/list/table-options.js`
- **无后端/API 变更**
- 其他使用 `Cat2BugTable` 的页面（计划、报告、文档等）未设置 `required` 时行为不变
