## MODIFIED Requirements

### REQ-2 导入模版列集

当 `exportScope=importTemplate` 时，输出列为 `visible=true` 或属于模块必填集合的列；顺序与 `exportColumns` 一致。必填列表头样式（红色）保持不变。

缺陷（defect）模块的导入模版必填 Java 字段集合 MUST 仅包含：`defectName`、`handleByNames`。`defectTypeImportName`、`defectStateImportName`、`defectLevel` MUST NOT 属于导入模版必填集合（空单元格在导入时由 `defect-field-defaults` 规则填充默认值）。

用例（case）模块必填集合行为不变。

#### Scenario: 缺陷导入模版类型状态非红头

- **WHEN** 用户下载缺陷导入模版且类型、状态、优先级列在表格配置中为可见
- **THEN** 类型、状态、优先级列表头不为必填红色；缺陷名称与处理人列表头为必填红色

#### Scenario: 缺陷导入模版仍包含类型状态列

- **WHEN** 用户表格配置中类型、状态、优先级列为可见
- **THEN** 导入模版仍导出这些列供填写，但允许留空
