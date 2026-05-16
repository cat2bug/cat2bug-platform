## Why

缺陷当前为物理删除，误删无法找回，且无法在列表中单独查看已删除记录。需要与若依体系一致的软删除（`del_flag`）、专用「已删除缺陷」页签，并在自定义页签中支持按删除状态筛选，同时保证表格/Excel 对已删数据的交互安全。

## What Changes

- 在 `sys_defect` 表新增 `del_flag` char(1) NOT NULL DEFAULT `'0'`（`0` 存在，`2` 删除；沿用项目惯例，不使用 `1`）
- 缺陷删除改为软删除（`UPDATE del_flag='2'`），不再 `DELETE` 物理行；删除与恢复操作写入 `sys_defect_log`（枚举末尾追加 `DELETE`、`RESTORE`）
- 新增恢复 API；列表/详情/统计等查询默认排除 `del_flag='2'`
- 缺陷页 Tab：在「全部缺陷」右侧增加不可删除的「已删除缺陷」内置页签；为「全部缺陷」「已删除缺陷」、自定义页签配置不同图标
- 新建缺陷页签对话框底部增加「已删除」开关（`el-switch`，`params.delFlag`，默认 `0`）
- 「全部缺陷」及 `del_flag=0` 的自定义页签不包含已删数据；`del_flag=2` 时仅显示已删数据
- 已删除列表：`table.vue` 操作列 `DefectTools` 仅显示**恢复**与**查看**按钮；Excel 视图中已删行不可编辑
- 新增 i18n 文案（含多语言 JSON）
- 不新增 `delete_time` / `delete_by` / `delete_by_id` 字段（审计依赖 `sys_defect_log`）

## Capabilities

### New Capabilities

- `defect-soft-delete`: 缺陷软删除、恢复、`del_flag` 数据模型、列表过滤、日志类型、API 及统计/关联查询的删除态约定
- `defect-deleted-tab-ui`: 缺陷页内置/自定义页签、图标、新建页签「已删除」开关、表格 `DefectTools` 恢复态、Excel 只读行

### Modified Capabilities

<!-- 无既有 openspec 规格涉及缺陷列表/删除行为 -->

## Impact

- **数据库**: H2/MySQL Flyway 迁移；`h2-schema.sql` / 初始化 SQL 同步
- **后端**: `SysDefect`、`SysDefectMapper.xml`、`SysDefectServiceImpl`、`SysDefectController`；`SysDefectLogStateEnum`；`SysDefectStatisticMapper`、`SysPlanMapper`、`ApiDefectMapper` 等所有 `sys_defect` 列表查询
- **前端**: `defect/index.vue`、`DefectTabDialog.vue`、`list/table.vue`、`list/excel.vue`、`DefectTools/index.vue`、`api/system/defect.js`、i18n 各语言文件；可选 SVG 图标
- **文档**: `readme/production/user-guide/current-project/defect/defect-tabs.md`、`defect-delete.md` 等用户指南
