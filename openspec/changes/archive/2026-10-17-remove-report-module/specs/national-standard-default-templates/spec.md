## REMOVED Requirements

### Requirement: Taxonomy maps standard documents to template shapes
**Reason**: 产品不再提供报告模版。
**Migration**: 无。标准文档种类的说明仍可由 `test-standards-skills` 覆盖，不在本能力保留。

### Requirement: Default templates are valid Cat2Bug Markdown DSL
**Reason**: 产品不再提供报告模版。
**Migration**: 无。

### Requirement: Default set covers primary reporting scenarios
**Reason**: 产品不再提供报告模版。
**Migration**: 无。

## ADDED Requirements

### Requirement: Capability is retired
The system MUST NOT provide this capability. Report pages, report APIs, report tables, and report permissions MUST NOT be reintroduced under this specification.

#### Scenario: Report surface is absent
- **WHEN** a maintainer looks for the behavior previously required by this specification
- **THEN** the product does not implement it
