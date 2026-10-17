## REMOVED Requirements

### Requirement: Report detail uses a three-pane shell
**Reason**: 产品不再提供报告详情。
**Migration**: 无。

### Requirement: Table of contents navigates to heading anchors
**Reason**: 该要求只约束报告详情。Git 仓库浏览若仍需要从 Markdown 抽标题，不由本能力规定。
**Migration**: 无。

### Requirement: Side panes can collapse on narrow layouts
**Reason**: 产品不再提供报告详情。
**Migration**: 无。

### Requirement: Export uses document body only
**Reason**: 产品不再提供报告导出。
**Migration**: 无。

## ADDED Requirements

### Requirement: Capability is retired
The system MUST NOT provide this capability. Report pages, report APIs, report tables, and report permissions MUST NOT be reintroduced under this specification.

#### Scenario: Report surface is absent
- **WHEN** a maintainer looks for the behavior previously required by this specification
- **THEN** the product does not implement it
