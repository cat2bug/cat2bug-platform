## Context

缺陷类型、优先级、状态在表格 `table-options`、Excel `COLS`、`DEFECT_TEMPLATE_REQUIRED`、`AddDefect` 表单 rules、`importDefect` 校验之间不一致。`insertSysDefect` 已强制新建状态为 `PROCESSING`，但未统一补全类型与优先级；Excel 内联创建与导入路径各自校验必填。

产品要求：创建/导入时可不填三字段，持久化前写入默认值（`BUG` / `middle` / `PROCESSING`）；编辑时类型与优先级清空则回落默认；**状态在编辑 UI 不可清空**。

## Goals / Non-Goals

**Goals:**

- 单一后端入口 `applyDefectDefaults(SysDefect defect)`（或等价工具类），在 insert、update、import 批量写入前调用。
- 前端各入口去掉三字段的必填约束，提交前可选双保险 normalize（与后端一致）。
- 导入模版必填集与表格红星、Excel `COLS.required` 对齐。
- HandleDefect / 编辑态状态控件 `clearable: false`。

**Non-Goals:**

- 修改用例模块必填规则。
- 修改缺陷工作流（指派、驳回等）状态机语义。
- Open API 文档大规模改写（可在 tasks 中记一条可选）。
- 历史脏数据批量迁移（空 type/level 的旧行若存在，可在读取展示时容错，不强制 SQL 修复）。

## Decisions

### 1. 默认值常量集中定义

- **选择**：在 `cat2bug-platform-common` 增加 `DefectDefaults`（或 `SysDefectConstants`）：
  - `DEFAULT_TYPE = SysDefectTypeEnum.BUG`
  - `DEFAULT_LEVEL = "middle"`
  - `DEFAULT_STATE = SysDefectStateEnum.PROCESSING`
- **理由**：前后端文档与单测可引用；避免魔法字符串散落。
- **替代**：仅前端默认 → API/导入可被绕过。

### 2. `applyDefectDefaults` 语义

- **选择**：
  - `defectType == null` → `BUG`
  - `defectLevel` 为 null 或 blank → `middle`
  - `defectState == null` → `PROCESSING`（update 时若调用方传入 null 才补；正常编辑 UI 不应传 null）
- **insertSysDefect**：在现有 `setDefectState(PROCESSING)` 处改为调用 `applyDefectDefaults`，避免重复赋值逻辑分叉。
- **updateSysDefect**：保存前 `applyDefectDefaults`，使清空 type/level 回落默认。
- **importDefect**：移除类型/状态空的 `emptyCell` 分支；解析失败仍进 `invalidCell`；解析成功或留空后统一 `applyDefectDefaults`。

### 3. 前端表格与 Excel

- **table-options**：移除 `type`、`state` 的 `required: true`；保留 `defect.name`、`handle-by`。
- **excel.vue**：`defectType`、`defectLevel` 的 `COLS.required = false`；`createDefectFromExcelPlaceholderRow` 与 `persistSheetCells` 在提交前补默认；`coerceDefectLevelValue` 空值时返回 `middle`。
- **状态列**：占位行可不填；已有 `defectId` 行编辑时不允许将 `defectStateText` 置为空字符串（拒绝保存或回滚上一值）。

### 4. 弹窗 AddDefect / HandleDefect

- **AddDefect**：删除 `defectType`、`defectLevel` 的 `rules.required`；`form` 初始 `defectType: 'BUG'`、`defectLevel: 'middle'`（与现 level 默认一致）；提交前 normalize。
- **HandleDefect**：同上 rules；状态 `el-select` 设置 `:clearable="false"`；类型/优先级 select 允许 clearable，保存时 normalize。

### 5. 导入模版必填

- **选择**：`DEFECT_TEMPLATE_REQUIRED` 改为仅 `defectName`、`handleByNames`（与产品仍必填字段一致）。
- **REQ-2**（excel-export-table-columns）：模版必填集合同步变更。

### 6. 与 `table-required-column-header` 归档决策的关系

- 原归档 design 写明「Excel COLS 不改」；本变更 **显式覆盖** 该非目标，三端对齐。

## Risks / Trade-offs

- **[Risk] 用户误以为类型/状态可长期为空** → 列表始终展示解析后的默认枚举/字典，不出现空白列。
- **[Risk] 导入模版旧版仍标红类型/状态** → 用户需重新下载模版；tasks 含文档说明。
- **[Risk] update 误将未传字段当 null 清空** → `applyDefectDefaults` 仅对 null/blank 补默认，不覆盖合法显式值；MyBatis update 动态 SQL 保持「字段非 null 才更新」语义。
- **[Trade-off] 编辑清空优先级 vs 状态不可清空** → 产品刻意区分；spec 分场景描述。

## Migration Plan

1. 先后端（defaults + import + insert/update），再前端各入口。
2. 无数据库 schema 变更。
3. 回滚：还原服务与前端必填配置即可。

## Open Questions

- 无（状态编辑不可清空、默认值、范围已由产品确认）。
