## REMOVED Requirements

### Requirement: Users can annotate report body by line or selection
**Reason**: 产品不再提供报告。
**Migration**: 删除 `module_type` 为 `report` 的评论，不迁移到其它模块。

### Requirement: Flat replies on annotation threads
**Reason**: 产品不再提供报告批注。
**Migration**: 无。

### Requirement: Anchor rematch after body edits
**Reason**: 产品不再提供报告批注。
**Migration**: 无。

### Requirement: Bidirectional navigation between body and comments
**Reason**: 产品不再提供报告批注。
**Migration**: 无。

### Requirement: Annotation delete authorization
**Reason**: 产品不再提供报告批注。评论表仍用于缺陷，缺陷评论的删除规则不变。
**Migration**: 无。

### Requirement: No notification side effects
**Reason**: 产品不再提供报告批注。
**Migration**: 无。

## ADDED Requirements

### Requirement: Capability is retired
The system MUST NOT provide this capability. Report pages, report APIs, report tables, and report permissions MUST NOT be reintroduced under this specification.

#### Scenario: Report surface is absent
- **WHEN** a maintainer looks for the behavior previously required by this specification
- **THEN** the product does not implement it
