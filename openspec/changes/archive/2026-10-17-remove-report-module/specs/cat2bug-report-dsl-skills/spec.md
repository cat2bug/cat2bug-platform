## REMOVED Requirements

### Requirement: DSL skills document all supported token forms
**Reason**: 产品不再提供报告 DSL。
**Migration**: 删除 `readme/development/skills/cat2bug-report-dsl/`。

### Requirement: DSL skills forbid inventing unsupported tokens
**Reason**: 产品不再提供报告 DSL。
**Migration**: 无。

### Requirement: Authoring rules for plan index and namespaces
**Reason**: 产品不再提供报告 DSL。
**Migration**: 无。

## ADDED Requirements

### Requirement: Capability is retired
The system MUST NOT provide this capability. Report pages, report APIs, report tables, and report permissions MUST NOT be reintroduced under this specification.

#### Scenario: Report surface is absent
- **WHEN** a maintainer looks for the behavior previously required by this specification
- **THEN** the product does not implement it
