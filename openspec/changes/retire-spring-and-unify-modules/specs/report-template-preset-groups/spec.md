## RENAMED Requirements

- FROM: `### Requirement: Presets served from Spring admin classpath`
- TO: `### Requirement: Presets served from admin classpath`

## MODIFIED Requirements

### Requirement: Presets served from admin classpath
Preset Markdown and preview images SHALL be packaged under `cat2bug-platform-admin/src/main/resources/报告/模版/` and exposed through authenticated report-template APIs. The inventory SHALL be declared in a manifest configuration file listing groups, keys, titles, content paths, and preview paths. Native builds MUST include these resources.

#### Scenario: List presets via API
- **WHEN** an authorized client requests the grouped template picker payload
- **THEN** the response includes preset items from the manifest with preview URLs or paths and without requiring those presets to exist as DB rows

#### Scenario: Read preset content
- **WHEN** an authorized client requests a preset by group and key
- **THEN** the system returns the Markdown file content from the classpath
