## Why

缺陷与用例列表已对表单必填字段对应的列使用 `Cat2BugTable` 的 `required: true` 红色表头，测试计划、交付物、文档管理三页尚未对齐，用户无法从列表一眼分辨必填项。

## What Changes

- 测试计划列表：`plan.name` 列配置 `required: true`
- 交付物列表：`module.name` 列配置 `required: true`
- 文档管理列表：`doc.name` 列配置 `required: true`
- 视觉与缺陷页一致：红色表头文字（及 pin 同色），无星号前缀；复用现有 `Cat2BugTable` 样式，不新增组件或主题变量
- **不做**：用例列表（已对齐）、执行计划抽屉子表、新建计划弹窗内用例表、文档「文件」字段（无对应列表列）

## Capabilities

### New Capabilities

- `list-required-column-header`: 业务列表对表单必填字段对应列启用 `required: true`，表头呈红色；本次覆盖测试计划 / 交付物 / 文档名称列

### Modified Capabilities

- （无）主规范中尚无独立「必填列表头」能力；表头手柄相关要求见既有 `cat2bug-table-header-handles`，本次不改其 REQUIREMENTS

## Impact

- 前端列配置：`plan-table-options.js`、`module/index.vue` 内 `MODULE_TABLE_COLUMNS`、`document-table-options.js`
- 无后端 / API / 数据库变更
- 列宽本地缓存合并以默认列为 base，补上 `required` 后旧缓存仍生效，无需清缓存
