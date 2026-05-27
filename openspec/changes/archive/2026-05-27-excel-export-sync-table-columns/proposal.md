# 提案：Excel 导出/导入模版与表格列配置同步

## 背景

用例、缺陷列表支持 Cat2BugTable 列顺序与显隐（本地缓存）。当前数据导出与导入模版由后端 `@Excel` 固定列集生成，与前端表格不一致。

## 目标

- **数据导出**：仅导出当前**可见**列，列顺序与表格配置一致；不强制补必填列。
- **导入模版**：列为 **可见 ∪ 必填**，顺序与表格配置一致；必填列保留红表头。
- **用例**：`defectProcessingCount`（关联缺陷）在数据导出中可随表格显隐导出；导入模版不含该列。

## 范围

- 用例：`/system/case/export`、`/system/case/importTemplate`
- 缺陷：`/system/defect/export`、`/system/defect/importTemplate`
- `ExcelUtil` 动态列序/白名单
- 前端传 `params.exportScope`、`params.exportColumns`

## 非目标

- 其他模块（用户、字典等）导出
- 改变导入校验规则
