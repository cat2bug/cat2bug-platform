## ADDED Requirements

### Requirement: 列表动态列

缺陷表格视图 MUST 为每个**已启用**自定义字段提供可选列；列标识 MUST 为 `custom:{fieldKey}`，列标题 MUST 为 `field_label`。

单元格渲染 MUST 通过 registry 的 `cell`/`display` 适配器格式化（枚举带颜色、图片缩略图等）。

#### Scenario: 表格显示枚举列

- **WHEN** 用户开启列 `custom:severity` 且缺陷值为 `P0`
- **THEN** 单元格显示配置中的 label 及颜色样式

### Requirement: 用户本地列显隐缓存

自定义列的显示/隐藏 MUST 并入现有本地缓存键 `defect-tabledefect-table-field-list`（与 `defect-table` cacheKey 一致），**不得**引入项目级强制列布局。

禁用字段（`enabled=0`）MUST NOT 出现在列选择器中。

#### Scenario: 隐藏自定义列

- **WHEN** 用户在列设置中取消勾选某自定义列
- **THEN** 表格与 Excel 视图均不再显示该列（共用同一份 field-list 缓存）

### Requirement: Excel 视图动态列

`excel.vue` MUST 根据启用字段定义动态扩展 `COLS`（或等价结构），并与表格列选择器使用同一套 picker 键集合。

可编辑性：除创建人、编号等系统只读列外，自定义列的可编辑性 MUST 与字段类型匹配（`image`/`file` 遵循现网 Excel 上传列交互）。

#### Scenario: Excel 与表格列顺序一致

- **WHEN** 用户调整列顺序并写入 field-list 缓存
- **THEN** Excel 视图列顺序与表格一致（复用现有同步逻辑）

### Requirement: 导出包含自定义列

导出 Excel MUST 包含用户当前可见的自定义列（与 field-list 中未隐藏列一致），表头为 `field_label`。

枚举导出值 MUST 为 **label**（非 key），便于人工阅读。

#### Scenario: 导出含自定义字段

- **WHEN** 列表显示 `custom:env` 列且缺陷有值 `prod`
- **THEN** 导出文件中对应列单元格为 `prod`

### Requirement: 导入解析自定义列

导入 MUST 支持通过表头匹配 `field_label` 或 `field_key`（优先 key 精确匹配，其次 label）；按字段类型校验并写入 `custom_fields`。

枚举导入 MUST 支持 **key** 精确匹配；若单元格等于某 option 的 **label** 则映射为对应 key。

图片/文件列导入 MVP：仅接受已有 URL 文本（逗号分隔），**不**解析嵌入二进制。

#### Scenario: 导入非法枚举

- **WHEN** 导入行枚举列值为「不存在的选择」
- **THEN** 该行报错且不入库（或整文件失败，与现网导入策略一致）

#### Scenario: 导入必填自定义字段为空

- **WHEN** 字段必填且导入单元格为空
- **THEN** 该行校验失败并提示字段名
