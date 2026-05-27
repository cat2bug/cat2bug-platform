## MODIFIED Requirements

### Requirement: 缺陷列表必填列

缺陷页表格列定义 MUST 将以下列标记为 `required: true`：缺陷名称（`defect.name`）、处理人（`handle-by`）。缺陷类型（`type`）、状态（`state`）、缺陷等级（`priority`）MUST NOT 标记为必填。

#### Scenario: 缺陷页必填列表头

- **WHEN** 用户在缺陷列表表格模式查看表头
- **THEN** 缺陷名称、处理人两列表头为红色；类型、状态、缺陷等级及其它列为默认色
