## 1. 数据库与领域模型

- [x] 1.1 Flyway：`sys_project_defect_field` 表（h2 + mysql）
- [x] 1.2 Flyway：`sys_defect.custom_fields` JSON 列（h2 + mysql）
- [x] 1.3 Domain / Mapper / XML：`SysProjectDefectField` CRUD
- [x] 1.4 `SysDefect` + `SysDefectMapper` 映射 `custom_fields`

## 2. 字段定义后端

- [x] 2.1 `ISysProjectDefectFieldService`：校验 field_key、类型、type_config、50 条上限
- [x] 2.2 Controller：`/system/project/{projectId}/defect-fields` 全套 API
- [x] 2.3 禁止改 field_key / 有数据禁止改 type
- [x] 2.4 单测：枚举 options 校验、软删、上限

## 3. 缺陷值校验与持久化

- [x] 3.1 保存/更新缺陷时合并校验 `custom_fields`（按启用定义）
- [x] 3.2 剥离未定义键；必填/类型/长度错误信息 i18n
- [x] 3.3 单测：各类型合法/非法样例

## 4. 项目设置 UI

- [x] 4.1 `DefectSettingsCard.vue` 注册到 `project/option/item`
- [x] 4.2 `views/system/project/defect-fields/index.vue` CRUD 页（含枚举 option 编辑器）
- [x] 4.3 `api/system/defect-field.js` + i18n 7 语言

## 5. 前端 registry 与表单/详情

- [x] 5.1 `components/DefectCustomField/registry.js` 与 editors/displays
- [x] 5.2 实现全类型 editor/display（含 array 行编辑、object JSON、image/file 上传）
- [x] 5.3 接入 `AddDefect.vue`、`EditDefectDialog.vue`
- [x] 5.4 接入 `HandleDefect.vue` 只读区

## 6. 列表与列缓存

- [x] 6.1 启动加载启用字段；生成 `custom:{fieldKey}` 列
- [x] 6.2 接入 `DefectTable` / `table.vue` 动态列与 formatter
- [x] 6.3 合并进 `defect-tabledefect-table-field-list` picker

## 7. Excel 导入导出

- [x] 7.1 `excel.vue` 动态 COLS 与单元格编辑/展示
- [x] 7.2 导出动态列（枚举 label）
- [x] 7.3 导入列匹配与校验（枚举 key/label）

## 8. Tab 与列表筛选

- [x] 8.1 `CustomFieldFilterBuilder`（或 filters/*.vue）供 `DefectTabDialog`
- [x] 8.2 Tab config 持久化 `customFieldFilters`
- [x] 8.3 `SysDefectMapper` JSON 筛选（H2 + MySQL）
- [x] 8.4 `defect-query-extension.js` 注册 `customFieldFilters`
- [x] 8.5 `defect/index.vue` Tab 激活时应用筛选
- [x] 8.6 集成测试：双库各 1 条筛选用例

## 9. 文档与验收

- [x] 9.1 补充 `readme/development/database-design.md` 两表说明
- [x] 9.2 完成 `TESTING.md` 手工路径
- [x] 9.3 归档前将 specs 同步至 `openspec/specs/`（archive 流程）
