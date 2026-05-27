# Spec: 表格必填列表头

## ADDED Requirements

### Requirement: Cat2BugTable 支持必填列标记

`Cat2BugTable` 的列配置对象 SHALL 支持可选属性 `required`（布尔）。当 `required` 为 `true` 时，该列表头显示的文字颜色 MUST 为 `#f56c6c`（或项目内等效的危险色变量）。表头 MUST NOT 自动添加 `*` 或其它前缀。

#### Scenario: 必填列表头为红色

- **WHEN** 列配置 `{ required: true, key: 'case.name', ... }` 且表格渲染表头
- **THEN** 该列表头可见文字为红色，且无星号前缀

#### Scenario: 非必填列表头样式不变

- **WHEN** 列配置未设置 `required` 或 `required` 为 `false`
- **THEN** 表头文字使用默认颜色（与变更前一致）

#### Scenario: 两种表头 DOM 均生效

- **WHEN** 列 `pinFixedToggle === false` 使用 `header-title-only`，或默认使用 `table-header` 内 `header-title`
- **THEN** 若 `required === true`，两种结构下的表头文字均为红色

### Requirement: 列缓存合并保留 required

从 localStorage 恢复列顺序与显隐时，系统 SHALL 从默认列定义（`columns` prop 传入的 defaults）合并 `required` 等业务字段；`required` MUST NOT 依赖缓存 JSON 中的字段。

#### Scenario: 用户自定义列顺序后必填样式仍在

- **WHEN** 用户拖拽调整列顺序并刷新页面
- **THEN** 仍为必填的列（defaults 中 `required: true`）表头保持红色

### Requirement: 用例列表必填列

用例页表格列定义 MUST 将以下列标记为 `required: true`：用例名称（`case.name`）、交付物（`module`）、预期（`expect`）。

#### Scenario: 用例页必填列表头

- **WHEN** 用户在用例列表表格模式查看表头
- **THEN** 用例名称、交付物、预期三列表头为红色；其余列（如步骤、图片）为默认色

### Requirement: 缺陷列表必填列

缺陷页表格列定义 MUST 将以下列标记为 `required: true`：缺陷名称（`defect.name`）、处理人（`handle-by`）。缺陷类型（`type`）、状态（`state`）、缺陷等级（`priority`）MUST NOT 标记为必填。

#### Scenario: 缺陷页必填列表头

- **WHEN** 用户在缺陷列表表格模式查看表头
- **THEN** 缺陷名称、处理人两列表头为红色；类型、状态、缺陷等级及其它列为默认色
