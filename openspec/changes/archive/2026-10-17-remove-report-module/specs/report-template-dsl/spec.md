## REMOVED Requirements

### Requirement: DSL storage format unchanged
**Reason**: 产品不再提供报告模版。
**Migration**: 无。`sys_report_template` 随迁移删除，内容不导出。

### Requirement: Variable token syntax
**Reason**: 产品不再提供报告模版。
**Migration**: 无。

### Requirement: Dynamic table token syntax
**Reason**: 产品不再提供报告模版。
**Migration**: 无。

### Requirement: Dynamic card token syntax
**Reason**: 产品不再提供报告模版。
**Migration**: 无。

### Requirement: Dynamic image token syntax
**Reason**: 产品不再提供报告模版。
**Migration**: 无。

### Requirement: Alignment and extended Markdown passthrough
**Reason**: 产品不再提供报告模版。
**Migration**: 无。

### Requirement: Unknown or legacy DSL fallback
**Reason**: 产品不再提供报告模版。
**Migration**: 无。

### Requirement: Render pipeline unchanged
**Reason**: 产品不再提供报告渲染。
**Migration**: 无。

## ADDED Requirements

### Requirement: Capability is retired
The system MUST NOT provide this capability. Report pages, report APIs, report tables, and report permissions MUST NOT be reintroduced under this specification.

#### Scenario: Report surface is absent
- **WHEN** a maintainer looks for the behavior previously required by this specification
- **THEN** the product does not implement it
