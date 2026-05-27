# 设计：Excel 导出列与表格同步

## 协议

前端 POST 时在 `params` 中附带：

| 参数 | 说明 |
|------|------|
| `exportScope` | `data`（数据导出）或 `importTemplate`（导入模版） |
| `exportColumns` | JSON 数组：`[{ "key", "prop", "visible" }]`，**完整列顺序**（含隐藏列） |

未传时保持现有行为（全部 `@Excel` 列，按 `sort` 排序）。

## 列过滤

```
foreach col in exportColumns（按数组顺序）:
  field = propToJavaField[col.prop]
  if scope == data && col.visible → 输出
  if scope == importTemplate && (col.visible || field ∈ required) → 输出
```

模版排除：`defectProcessingCount`（用例统计列，不可导入）。

## 字段映射（prop → Java 字段名）

### 缺陷 — 数据导出（SysDefect）

| prop | field |
|------|-------|
| projectNum | projectNum |
| defectTypeName | defectType |
| defectName | defectName |
| defectLevel | defectLevel |
| defectState | defectState |
| moduleName | moduleName |
| moduleVersion | moduleVersion |
| defectDescribe | defectDescribe |
| imgUrls | imgUrls |
| annexUrls | annexUrls |
| updateTime | updateTime |
| planStartTime | planStartTime |
| planEndTime | planEndTime |
| createMember | createMemberName |
| handleBy | handleByNames |

### 缺陷 — 导入模版（SysDefectImportTemplate）

| prop | field |
|------|-------|
| defectTypeName | defectTypeImportName |
| defectName | defectName |
| defectLevel | defectLevel |
| defectState | defectStateImportName |
| moduleName | moduleName |
| moduleVersion | moduleVersion |
| defectDescribe | defectDescribe |
| imgUrls | imgObjects |
| handleBy | handleByNames |

必填：`defectName`, `defectTypeImportName`, `defectStateImportName`, `handleByNames`

### 用例 — 数据导出（SysCase）

| prop | field |
|------|-------|
| caseNum | caseNum |
| caseName | caseName |
| moduleName | moduleId |
| caseLevel | caseLevelName |
| casePreconditions | casePreconditions |
| caseStep | caseStep |
| caseData | caseData |
| caseExpect | caseExpect |
| imgUrls | imgUrls |
| annexUrls | annexUrls |
| updateTime | caseExportUpdateTime |
| defectProcessingCount | defectProcessingCount |

### 用例 — 导入模版（SysCase IMPORT 字段）

| prop | field |
|------|-------|
| caseName | caseName |
| moduleName | moduleName |
| caseLevel | caseLevel |
| casePreconditions | casePreconditions |
| caseStep | caseStepScript |
| caseData | caseData |
| caseExpect | caseExpect |
| imgUrls | imgObjects |

必填：`caseName`, `moduleName`, `caseExpect`

## ExcelUtil

新增 `includeFields(String... orderedNames)`：仅输出白名单字段，顺序与参数一致；设置后不再按 `@Excel.sort` 重排。

## 前端

- `utils/excel-export-columns.js`：读缓存、合并默认列、生成 params
- `Cat2BugTable.getColumnConfigForExport()`
- 用例/缺陷导出与模版下载注入 params
