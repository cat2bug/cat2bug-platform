## Why

缺陷与测试用例的 Excel 导入原先依赖「当前界面语言」匹配表头与单元格显示值；用户用某一语言下载模板、填写数据后，切换 i18n 再导入会导致列无法映射或状态/类型/等级解析失败。部分修复（`c35c4692`）已覆盖缺陷表头多语言与状态/类型枚举，但尚未形成完整、可维护的通用能力，且缺陷优先级、用例历史列名等仍有缺口。需要在规范层面明确：**任意已支持语言的模板表头与下拉文案，在任意界面语言下均可成功导入**。

## What Changes

- 巩固并文档化 `ExcelUtil` 表头多语言候选匹配（当前语言、`@Excel.name` 默认值、各 `messages_*` 译文）。
- 扩展 `@Excel` 或等价机制，支持同一列的**备用 i18n key**（兼容 `case.name_excel` / `case.expected_excel` 等历史模板）。
- 为缺陷**优先级（字典 `defect_level`）**增加与 `DefectImportLabelResolver` 一致的多语言反向解析；修正 `DefectLevelHandler` 导入路径勿将单元格文本当作 message key。
- 确认测试用例导入在表头与业务校验上满足同等规则；补充用例相关回归场景。
- 统一 `LocaleUtils.MESSAGE_BUNDLE_LOCALES` 与 i18n 资源维护约定（新语言须同步列入）。
- 前端导入上传在打开/提交时刷新 `language` 请求头（缺陷已做，用例对齐）。
- 不改动对外 REST API 路径与请求体结构；不引入 **BREAKING** 行为变更。

## Capabilities

### New Capabilities

- `excel-import-i18n`: Excel 导入（缺陷、测试用例）在表头匹配与本地化单元格值解析上与当前 UI 语言解耦；支持项目已提供的全部 `messages_*` 语言。

### Modified Capabilities

- （无）本次为新增能力规格，不修改 `openspec/specs/` 下既有 capability 的需求定义。

## Impact

- **后端**: `cat2bug-platform-common`（`ExcelUtil`、`LocaleUtils`、各类 `*Handler` / `*ImportLabelResolver`）、`cat2bug-platform-system`（`SysDefectServiceImpl`、`SysCaseServiceImpl`）、`@Excel` 注解（若扩展备用 key）。
- **前端**: `DefectImport.vue`、`cat2bug-platform-ui/src/views/system/case/index.vue` 等导入上传组件的 `language` 头同步。
- **i18n**: `cat2bug-platform-admin/src/main/resources/i18n/messages*.properties`；`scripts/sync-i18n-messages.py` 文档约定。
- **测试**: 手工或自动化覆盖「A 语言模板 + B 语言界面导入」矩阵（至少中/英各一组）。
