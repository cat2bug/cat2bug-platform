## Context

Cat2Bug 使用 `ExcelUtil` + 实体 `@Excel` 注解完成缺陷（`SysDefect`）与测试用例（`SysCase`）的模板导出与导入。表头与下拉选项通过 `i18nNameKey` 与 `MessageUtils` 随请求头 `language` 本地化。导入链路分两层：

1. **表头 → 字段**：读取 Excel 第一行，与 `@Excel` 元数据匹配列索引。
2. **单元格 → 领域值**：Handler 或 Service 将显示文案解析为枚举、字典码、模块 ID 等。

提交 `c35c4692` 已实现 `collectImportHeaderCandidates`（遍历 `LocaleUtils.MESSAGE_BUNDLE_LOCALES`）及 `DefectImportLabelResolver`（缺陷状态/类型）。缺陷优先级仍经 `DefectLevelHandler` 误用 `MessageUtils.message(单元格文本)`；用例存在 `case.name` vs `case.name_excel` 等历史 key 分裂。本设计在现有实现上补全缺口并固化扩展点。

## Goals / Non-Goals

**Goals:**

- 表头匹配与 UI 当前语言无关：文件表头为任一已支持语言译文（含 `@Excel.name` 中文默认）均可映射到正确字段。
- 缺陷状态、类型、优先级单元格为任一已支持语言显示名时，均能解析为正确的枚举/字典值。
- 测试用例导入满足表头规则；级别（P0–P4 / 数字）保持语言无关行为。
- 新语言加入时，仅需维护 `messages_*` 与 `MESSAGE_BUNDLE_LOCALES`，无需改 Handler 硬编码分支。
- 前端导入请求在打开/提交对话框时携带与界面一致的 `language` 头（用于错误提示与导出一致性，不用于限制表头匹配）。

**Non-Goals:**

- 不支持未列入 `MESSAGE_BUNDLE_LOCALES` 的语言。
- 不改变 Excel 文件格式、REST API 路径或批量导入行数限制。
- 不国际化「交付物路径」「成员昵称」等项目内数据（仍为精确字符串匹配）。
- 不在本变更中重构整个 `ExcelUtil` 或替换 POI 方案。

## Decisions

### 1. 表头：延续并增强 `collectImportHeaderCandidates`

**选择**：在 `ExcelUtil` 中保持「当前语言 + `attr.name()` + 各 locale 的 `i18nNameKey` 译文」候选集；新增 `@Excel.alternateI18nKeys()`（`String[]`，默认空）合并历史 key 的各语言译文。

**理由**：集中在一处，缺陷/用例/未来实体共用；比在每个实体重复列举别名更简单。

**备选**：仅合并 i18n 资源（废弃 `case.name_excel`）——会破坏用户手中旧模板，故采用备用 key。

### 2. 单元格：按类型使用 `ImportLabelResolver` 模式

**选择**：

- 枚举（缺陷状态/类型）：沿用并完善 `DefectImportLabelResolver`（MessageSource × 全 locale + 枚举名 + 必要别名）。
- 字典（缺陷等级）：新增 `DefectLevelImportLabelResolver`，构建 `显示名/字典 label/dict_value` → `dict_value` 映射；`DefectLevelHandler` 导入分支调用 resolver，导出分支不变。
- 纯文本（用例名称、预期等）：无 resolver，依赖表头匹配即可。

**理由**：与已验证的缺陷状态方案一致；字典与枚举数据源不同，不宜塞进同一 Map。

**备选**：导入模板增加隐藏第二行英文字段名——用户可见性差，不采用。

### 3. `MESSAGE_BUNDLE_LOCALES` 为唯一语言清单

**选择**：表头候选与 label resolver 均遍历 `LocaleUtils.MESSAGE_BUNDLE_LOCALES`；在 `i18n/README.md` 中明确：新增 `messages_xx.properties` 时必须同步更新该数组。

**理由**：避免 `MessageSource.getAvailableLocales()` 与项目实际文件不一致。

### 4. 缺陷导入校验位置

**选择**：状态/类型继续在 `SysDefectServiceImpl.importDefect` 使用 resolver（IMPORT 字段为 `String`）；等级在 Handler 或 Service 单层统一解析为 `dict_value` 再入库。

**理由**：IMPORT 字段未挂 `DefectStateHandler`，与现有结构一致。

### 5. 前端 `language` 头

**选择**：用例导入对话框对齐 `DefectImport.vue`：打开/提交前调用 `setHeader` 刷新 `language`。

**理由**：错误消息与导出模板语言一致；表头匹配不依赖该头，但减少用户困惑。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 不同语言译文碰撞（同一字符串对应两个枚举） | 构建 Map 时后写覆盖；审查 messages 冲突；单元测试覆盖主要语言 |
| `alternateI18nKeys` 遗漏导致旧模板失败 | 在 spec 中列出用例/缺陷必填列的备用 key；文档说明废弃类 `SysCaseExcelTepmplate` 的 key |
| 字典 `dict_label`（中文「急」）与 message key（`urgent`）不一致 | Resolver 同时索引 `dict_label`、`dict_value`、各 locale message |
| 性能：每次导入构建 Map | 使用 static volatile + 双重检查锁（与 `DefectImportLabelResolver` 相同） |
| 新语言未加入 `MESSAGE_BUNDLE_LOCALES` | README + code review 清单；可选单测断言 locale 数组与 i18n 文件一致 |

## Migration Plan

1. 合并实现后，现有「当前语言模板」导入行为不变。
2. 用户持有的其他语言模板无需重新下载即可导入（满足 spec 矩阵即验收）。
3. 无数据库迁移；部署为常规前后端发布。
4. 回滚：还原 `ExcelUtil`/resolver 改动即可，无数据副作用。

## Open Questions

- 是否在 `@Excel` 上使用 `alternateI18nKeys` 还是将 `case.name_excel` 合并进 `case.name` 并仅保留备用 key 机制？**建议保留备用 key**，避免改翻译影响导出列标题文案。
- 是否需要为缺陷/用例导入增加自动化集成测试（上传 fixture xlsx）？**建议至少 2 个单测**：表头候选命中、等级/状态 resolver 多语言命中。
