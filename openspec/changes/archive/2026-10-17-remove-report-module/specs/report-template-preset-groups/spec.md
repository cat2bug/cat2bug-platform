## REMOVED Requirements

### Requirement: Picker lists templates in three groups
**Reason**: 产品不再提供报告模版选择器。
**Migration**: 无。

### Requirement: Standard presets are copy-only
**Reason**: 产品不再提供国标预制模版。
**Migration**: 删除 classpath `报告/模版/` 与同步脚本。

### Requirement: Copy preset into custom
**Reason**: 产品不再提供报告模版。
**Migration**: 无。

### Requirement: Full preset corpus from skills
**Reason**: 产品不再提供报告模版。
**Migration**: 删除 `readme/development/skills/report-templates/` 中仅服务于本模块的模版正文。

### Requirement: Preview images generated and configured
**Reason**: 产品不再提供报告模版。
**Migration**: 无。

### Requirement: Generate report can use a preset
**Reason**: 产品不再生成报告。
**Migration**: 无。

### Requirement: Presets served from admin classpath
**Reason**: 产品不再提供报告模版。
**Migration**: Native 资源列表去掉 `报告/**`。

## ADDED Requirements

### Requirement: Capability is retired
The system MUST NOT provide this capability. Report pages, report APIs, report tables, and report permissions MUST NOT be reintroduced under this specification.

#### Scenario: Report surface is absent
- **WHEN** a maintainer looks for the behavior previously required by this specification
- **THEN** the product does not implement it
