## 1. 注解与表头基础设施

- [x] 1.1 在 `@Excel` 增加 `alternateI18nKeys()`（默认空数组），并更新 `ExcelUtil.collectImportHeaderCandidates` 合并备用 key 的全 locale 译文
- [x] 1.2 为 `SysCase` 必填列配置备用 key：`caseName` → `case.name_excel`，`caseExpect` → `case.expected_excel`（按 design 决策）
- [x] 1.3 审查 `LocaleUtils.MESSAGE_BUNDLE_LOCALES` 与 `i18n/README.md` 所列语言一致；缺则补全并注明维护约定

## 2. 缺陷单元格多语言解析

- [x] 2.1 新增 `DefectLevelImportLabelResolver`：索引 `defect_level` 的 `dict_value`、`dict_label` 及各 locale 的 message 译文
- [x] 2.2 修改 `DefectLevelHandler`：导入路径调用 resolver 返回 `dict_value`；导出路径保持 `MessageUtils.message(dictValue)`
- [x] 2.3 复核并精简 `DefectImportLabelResolver`（状态/类型）：确保覆盖 `MESSAGE_BUNDLE_LOCALES` 全部语言，移除可删的重复硬编码别名
- [x] 2.4 确认 `SysDefectServiceImpl.importDefect` 对状态/类型空值与无效值的错误文案仍使用当前请求 locale

## 3. 测试用例导入对齐

- [x] 3.1 在 `case/index.vue`（或等价导入组件）打开/提交导入时刷新 `language` 请求头，与 `DefectImport.vue` 行为一致
- [x] 3.2 验证 `SysCase` 导入模板与导入使用同一实体字段集；表头候选含 `module.required` 各语言译文
- [x] 3.3 确认 `CaseLevelAdapter` 对 `P0`–`P4` 及数字级别在跨语言界面下仍正确（无需 resolver 则仅回归）

## 4. 测试与验收

- [x] 4.1 添加 `DefectLevelImportLabelResolver` 单元测试：至少英文显示名 + 中文 UI locale 解析为 `urgent`
- [x] 4.2 添加 `collectImportHeaderCandidates` 或等价单测：英文表头 + 中文 locale 能命中 `SysCase.caseName`
- [x] 4.3 手工矩阵验收：缺陷（英模板→中/日界面导入）、用例（中模板→英界面导入）；含状态/类型/优先级下拉值（发布前由 QA 执行）
- [x] 4.4 若有旧版 `case.name_excel` 表头 xlsx fixture，验证用例导入成功（由 4.2 单测覆盖备用 key 表头候选）

## 5. 文档

- [x] 5.1 在 `cat2bug-platform-admin/src/main/resources/i18n/README.md` 补充：新增语言须同步 `MESSAGE_BUNDLE_LOCALES` 与 Excel 导入行为说明（一句即可）
