## REMOVED Requirements

### Requirement: Milkdown editor integration in template edit page
**Reason**: 产品不再提供报告模版编辑。
**Migration**: 无。前端移除 Milkdown 与模版编辑页。

### Requirement: Split edit and preview layout
**Reason**: 产品不再提供报告模版编辑。
**Migration**: 无。

### Requirement: Toolbar parity with MarkdownTools
**Reason**: 产品不再提供报告模版编辑。
**Migration**: 无。

### Requirement: Visual block nodes for high-frequency DSL
**Reason**: 产品不再提供报告模版编辑。
**Migration**: 无。

### Requirement: Phase-gated delivery
**Reason**: 产品不再提供报告模版编辑。
**Migration**: 无。

### Requirement: Coexistence with legacy editor during migration
**Reason**: 产品不再提供报告模版编辑。
**Migration**: 无。

### Requirement: Automated test coverage
**Reason**: 产品不再提供报告模版编辑。
**Migration**: 删除对应测试，不改为其它模块的覆盖要求。

## ADDED Requirements

### Requirement: Capability is retired
The system MUST NOT provide this capability. Report pages, report APIs, report tables, and report permissions MUST NOT be reintroduced under this specification.

#### Scenario: Report surface is absent
- **WHEN** a maintainer looks for the behavior previously required by this specification
- **THEN** the product does not implement it
