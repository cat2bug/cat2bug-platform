## 1. 后端默认值与持久化

- [x] 1.1 新增 `DefectDefaults`（或等价工具类）：`BUG` / `middle` / `PROCESSING` 常量与 `applyDefectDefaults(SysDefect)`
- [x] 1.2 `insertSysDefect` 改为调用 `applyDefectDefaults`，移除重复的硬编码状态赋值分叉
- [x] 1.3 `updateSysDefect` 保存前调用 `applyDefectDefaults`
- [x] 1.4 `importDefect`：类型/状态/优先级空单元格不再进 `emptyCell`；非法非空值仍报错；批量插入前对每条 `applyDefectDefaults`
- [x] 1.5 更新 `ExcelColumnExportSupport.DEFECT_TEMPLATE_REQUIRED` 为 `defectName`、`handleByNames`
- [x] 1.6 补充/调整单测：`DefectDefaults` 或 `importDefect` 空类型/状态/优先级场景

## 2. 表格与 Excel 前端

- [x] 2.1 `table-options.js`：移除 `type`、`state` 的 `required: true`
- [x] 2.2 `excel.vue`：`COLS` 去掉 `defectType`、`defectLevel` 的 `required`；`EXCEL_REQUIRED_EMPTY_MSG` 同步
- [x] 2.3 `excel.vue`：占位行创建与 `persistSheetCells` 提交前补 `BUG` / `middle`；`coerceDefectLevelValue` 空值返回 `middle`
- [x] 2.4 `excel.vue`：已有 `defectId` 行编辑状态列不允许置空（拒绝或回滚）

## 3. 弹窗新建与编辑

- [x] 3.1 `AddDefect.vue`：移除 `defectType`、`defectLevel` 的 rules.required；初始默认 `BUG` / `middle`；提交前 normalize
- [x] 3.2 `HandleDefect.vue`：同上 rules 与 normalize；状态 `el-select` 设置 `clearable: false`
- [x] 3.3 若 `EditDefectDialog.vue` 共用相同 rules，一并调整

## 4. 导入导出联调与验证

- [x] 4.1 确认 `DefectImport.vue` 下载模版红头与导入行为符合新必填集
- [ ] 4.2 手工验证：表格模式表头红星、Excel 空类型/优先级创建、导入空三字段、HandleDefect 清空类型/优先级、状态不可清空
- [x] 4.3 运行相关 `mvn test` 与前端 lint（触及文件）
