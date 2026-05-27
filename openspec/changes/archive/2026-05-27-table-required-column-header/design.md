# 设计：表格必填列表头红色标识

## Context

- `Cat2BugTable` 为项目内列表页通用表格，列定义来自各页面的 `*-table-options.js`，显隐与顺序存于 localStorage（`key` / `fixed` / `visible`）。
- Excel 导入必填与 `ExcelColumnExportSupport` 中 `CASE_TEMPLATE_REQUIRED`、`DEFECT_TEMPLATE_REQUIRED` 一致；缺陷 Excel 内联编辑的 `COLS.required` 与导入不完全一致（等级列），本次不修改。
- 产品决策：表头**仅红色文字**，不加 `*`；缺陷**等级**不是必填项。

## Goals / Non-Goals

**Goals:**

- 列配置 `required: true` 时，表头文案为红色
- 用例、缺陷表格模式必填列与导入校验字段对齐
- 不影响未声明 `required` 的 Cat2BugTable 消费者

**Non-Goals:**

- 修改缺陷 Excel 视图 `excel.vue` 的 `COLS`
- 列选择器（显示字段）内必填标记
- 表头背景色、单元格校验提示
- 与 Excel 导出层共享单一常量源（可后续重构）

## Decisions

### 1. 在列对象上增加 `required: boolean`

- **选择**：`{ key, prop, required?, ... }`，默认视为 `false`
- **理由**：与缺陷 Excel 视图 `COLS` 命名一致；声明式、易在读 options 文件时维护
- **替代**：从 prop 名硬编码判断 → 分散、易与导入规则漂移

### 2. 样式仅在 Cat2BugTable 内实现

- **选择**：表头 slot 内对 `col.required` 绑定 class `header-title--required`，SCSS：`color: #f56c6c`
- **覆盖**：`table-header .header-title` 与 `header-title-only` 两种表头 DOM
- **理由**：一处实现，所有页面受益；颜色与 Element UI danger、缺陷 Excel 表头一致

### 3. 缓存合并保留 defaults 上的 `required`

- **选择**：不将 `required` 写入 localStorage；`mergeCachedColumns` 继续 `...base` 合并
- **理由**：必填是业务元数据，不应因用户拖拽/显隐而丢失

### 4. 必填列清单（与导入一致）

| 模块 | `required: true`（key / prop） |
|------|------------------------------|
| 用例 | `case.name` / `caseName`，`module` / `moduleName`，`expect` / `caseExpect` |
| 缺陷 | `type` / `defectTypeName`，`defect.name` / `defectName`，`state` / `defectState`，`handle-by` / `handleBy` |

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 表格与 Excel 视图必填列不一致（等级） | 文档注明；用户后续单独改 Excel `COLS` |
| 三处维护必填集合（options / ExcelColumnExportSupport / @Excel） | 本次仅改 options；后续可抽共享模块 |
| 红色在自定义主题下对比度不足 | 使用项目既有 `#f56c6c` |

## Migration Plan

- 纯前端发布，无数据迁移
- 回滚：移除 `required` 标记与 Cat2BugTable 样式即可

## Open Questions

（无）
