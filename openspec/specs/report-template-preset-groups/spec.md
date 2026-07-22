# report-template-preset-groups Specification

## Purpose

Present report templates in three picker groups (custom, ISO/IEC/IEEE 29119-3 presets, ISO 9001 presets) with copy-only presets and editable custom copies.

## Requirements
### Requirement: Picker lists templates in three groups
The report template picker (generate-report popover and equivalent template browser) SHALL present templates in this order: (1) custom templates for the current project, (2) ISO/IEC/IEEE 29119-3:2021 presets, (3) ISO 9001 presets. Group titles SHALL be user-visible labels (i18n allowed).

#### Scenario: Open generate-report template picker
- **WHEN** a user opens the template selection UI for a project
- **THEN** the UI shows the three groups in the order custom → 29119-3 → ISO 9001

#### Scenario: Add template only in custom
- **WHEN** the user uses add-template from the picker
- **THEN** a new editable custom template is created for the project and standard preset groups are unchanged

### Requirement: Standard presets are copy-only
Preset templates in the 29119-3 and ISO 9001 groups SHALL NOT be editable or deletable in the product UI or via update/delete APIs. The UI SHALL expose copy (and selection to generate a report) but MUST NOT expose edit or delete controls for presets.

#### Scenario: Preset card actions
- **WHEN** the user views a preset template card
- **THEN** copy is available and edit/delete controls are not offered

#### Scenario: Reject preset mutation
- **WHEN** a client attempts to update or delete a preset as if it were a stored custom template
- **THEN** the system rejects the mutation and the preset content on classpath remains unchanged

### Requirement: Copy preset into custom
Copying a preset SHALL create a new custom `sys_report_template` row for the current project whose content is taken from the preset Markdown on the classpath. The copy SHALL be editable and deletable like any custom template.

#### Scenario: Copy 29119-3 preset
- **WHEN** the user copies a 29119-3 preset
- **THEN** a new custom template appears under the custom group with the preset body as initial content

#### Scenario: Copy ISO 9001 preset
- **WHEN** the user copies an ISO 9001 preset (manual or record)
- **THEN** a new custom template appears under the custom group and can be edited or deleted

### Requirement: Presets served from Spring admin classpath
Preset Markdown and preview images SHALL be packaged under `cat2bug-platform-admin/src/main/resources/报告/模版/` (or the path finalized in design if filesystem issues force ASCII rename) and exposed through authenticated report-template APIs. The inventory SHALL be declared in a manifest configuration file listing groups, keys, titles, content paths, and preview paths.

#### Scenario: List presets via API
- **WHEN** an authorized client requests the grouped template picker payload
- **THEN** the response includes preset items from the manifest with preview URLs or paths and without requiring those presets to exist as DB rows

#### Scenario: Read preset content
- **WHEN** an authorized client requests a preset by group and key
- **THEN** the system returns the Markdown file content from the classpath

### Requirement: Full preset corpus from skills
The packaged presets SHALL include all business templates from `readme/development/skills/report-templates/29119-3/` and all files under `9001/manuals/` and `9001/records/` that are template Markdown bodies (excluding skill index/overview-only docs that are not report templates).

#### Scenario: Corpus coverage
- **WHEN** an agent or tester compares skills template files to the manifest
- **THEN** every included 29119-3 template and every 9001 manuals/records template has a manifest entry

### Requirement: Preview images generated and configured
Each preset SHALL have a preview PNG produced by an HTML-to-PNG batch process and referenced from the manifest. If generation fails for an item, the UI MAY fall back to a placeholder image without failing the whole picker.

#### Scenario: Manifest references PNG
- **WHEN** the sync/preview script completes successfully for a template
- **THEN** the manifest preview path points at a PNG sibling (or configured path) for that template key

### Requirement: Generate report can use a preset
Selecting a preset to generate a report SHALL use classpath preset content (via key) and MUST NOT require the preset to have been copied first, unless product UX explicitly requires copy-first (default: allow generate from preset directly using classpath content).

#### Scenario: Generate from preset card
- **WHEN** the user activates a preset card to create a report
- **THEN** the created report body is initialized from the preset Markdown on the classpath

