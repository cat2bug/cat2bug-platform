## ADDED Requirements

### Requirement: Tab 配置扩展 customFieldFilters

`sys_project_defect_tabs.config` MUST 支持可选属性 `customFieldFilters`，类型为筛选条件数组，每项含 `fieldKey`、`op`、`value`（`value` 形态由 `op` 与字段类型决定）。

`DefectTabDialog` MUST 为每个已启用且**支持筛选**的字段渲染筛选控件（见 design 运算符表）。

保存 Tab 时 MUST 持久化 `customFieldFilters`。

#### Scenario: Tab 保存枚举筛选

- **WHEN** 用户创建 Tab 并设置自定义字段 `severity` 为 `in [P0, P1]`
- **THEN** `config.customFieldFilters` 含对应条目
- **THEN** 激活该 Tab 时列表请求携带筛选参数

### Requirement: 列表查询 customFieldFilters 参数

缺陷列表查询 MUST 接受 `params.customFieldFilters`（JSON 数组），服务端按项目字段定义解析并追加 SQL 条件。

仅 `enabled=1` 的字段 MUST 允许参与筛选；未知 `fieldKey` MUST 忽略。

#### Scenario: 字符串包含筛选

- **WHEN** `customFieldFilters` 含 `{ fieldKey: "env", op: "contains", value: "prod" }`
- **THEN** 结果集仅含 `custom_fields.env` 字符串包含 `prod` 的缺陷（当前项目）

#### Scenario: 枚举 in 筛选

- **WHEN** `op` 为 `in` 且 `value` 为 `["P0","P1"]`
- **THEN** 结果匹配枚举 key 属于该集合的缺陷

### Requirement: 复杂类型筛选 MVP

对 `object`、`image`、`file` 类型，MVP MUST 至少支持 `isEmpty` 与 `isNotEmpty`。

对 `array` 类型，MVP MUST 支持 `isEmpty`、`isNotEmpty` 与 `contains`（将数组 JSON 字符串化后子串匹配，文档注明性能限制）。

#### Scenario: 筛选「有附件」

- **WHEN** 用户对 `file` 类型字段使用 `isNotEmpty`
- **THEN** 返回 `custom_fields` 中该键为非空数组的缺陷

### Requirement: 查询扩展参数清理

`defect-query-extension.js` MUST 将 `customFieldFilters` 注册为扩展键；在 `clearExtensionParams`、`Tab` 切换、以及无扩展的 `searchQuery` 调用时 MUST 清除该键，避免筛选泄漏。

#### Scenario: 切换 Tab 清除自定义筛选

- **WHEN** 用户从带 `customFieldFilters` 的自定义 Tab 切换到「全部」
- **THEN** 后续请求不包含上一 Tab 的 `customFieldFilters`

### Requirement: 双库 JSON 查询一致性

`SysDefectMapper` 对 `customFieldFilters` 的实现 MUST 在 H2 与 MySQL profile 下行为一致（至少覆盖：`eq`、`contains`、`isEmpty` 各一例）。

#### Scenario: H2 环境 Tab 筛选

- **WHEN** 开发环境使用 H2 且激活带自定义筛选的 Tab
- **THEN** 列表返回结果与 MySQL 环境下相同测试数据一致
