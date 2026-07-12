# report-template-dsl Specification

## ADDED Requirements

### Requirement: DSL storage format unchanged

The system SHALL persist report template content as plain-text Markdown with embedded Cat2Bug DSL tokens in `sys_report_template.template_content`. The Milkdown editor MUST NOT introduce a separate JSON/binary document format for storage.

#### Scenario: Save after Milkdown edit

- **WHEN** a user saves a report template edited in the Milkdown editor
- **THEN** the stored `template_content` MUST be a UTF-8 string containing valid Cat2Bug DSL syntax recognizable by existing `markdown-it` plugins

#### Scenario: Project export compatibility

- **WHEN** a project is exported and includes `report_template.json`
- **THEN** template entries MUST use the same `template_content` string format as before this change

### Requirement: Variable token syntax

The DSL SHALL support dot-path variable tokens in the form `${api.<namespace>.<field>}` and plan-indexed tokens `${api.plan.<field>[<n>]}` where `<n>` is a 1-based plan index.

#### Scenario: Project variable round-trip

- **WHEN** a template contains `${api.project.projectName}`
- **THEN** parse → serialize MUST reproduce the exact token string `${api.project.projectName}`

#### Scenario: Plan variable with index round-trip

- **WHEN** a template contains `${api.plan.defectCount[2]}`
- **THEN** parse → serialize MUST preserve the field name and plan index `[2]`

### Requirement: Dynamic table token syntax

The DSL SHALL support dynamic table tokens `$table{<dataSource>[<columnSpec>]}` where `<dataSource>` is `api.defect.list` or `api.case.list`, and `<columnSpec>` is a comma-separated list of `field` or `field:label` entries.

#### Scenario: Defect table default columns round-trip

- **WHEN** a template contains the toolbar-default defect table token with 14 columns
- **THEN** parse → serialize MUST preserve data source, column order, and field names

#### Scenario: Column alias round-trip

- **WHEN** a template contains `$table{api.defect.list[defectName:缺陷名称,defectLevel:等级]}`
- **THEN** parse → serialize MUST preserve both field keys and display labels (including non-ASCII labels)

### Requirement: Dynamic card token syntax

The DSL SHALL support dynamic card tokens `$card{api.case.list}` and `$card{api.defect.list}` as atomic block-level constructs.

#### Scenario: Case card round-trip

- **WHEN** a template contains `$card{api.case.list}`
- **THEN** parse → serialize MUST reproduce `$card{api.case.list}` exactly

### Requirement: Dynamic image token syntax

The DSL SHALL support image tokens `$image["style":"<css>"]{<dataPath>}` where `<dataPath>` references project-bound image fields (e.g. `api.project.projectIcon`).

#### Scenario: Project icon image round-trip

- **WHEN** a template contains `$image["style":"width:200px;height:200px;"]{api.project.projectIcon}`
- **THEN** parse → serialize MUST preserve style attribute and data path

### Requirement: Alignment and extended Markdown passthrough

The DSL parser/serializer SHALL pass through alignment markers (`:--`, `-:-`, `--:`) and extended inline syntax (`++underline++`, `~~strike~~`, `==flag==`, `^sup^`, `~sub~`) without structural corruption.

#### Scenario: Alignment line preserved

- **WHEN** a template contains a line `:-- 左对齐文字`
- **THEN** parse → serialize MUST preserve the alignment prefix and text content

### Requirement: Unknown or legacy DSL fallback

When the editor encounters DSL constructs not yet modeled as Milkdown nodes, it MUST preserve the original source text verbatim on round-trip.

#### Scenario: Plan statistics mega-table preserved

- **WHEN** a template contains a wide native Markdown table with many embedded `${api.plan.*[n]}` cells
- **THEN** opening and saving in Milkdown MUST NOT alter cell variable tokens or table structure

#### Scenario: Unrecognized token preserved

- **WHEN** a template contains a valid `$table{api.defect.list[...]}` variant that the visual editor cannot yet edit
- **THEN** serialize MUST still output the original token text if the user made no edits to that block

### Requirement: Render pipeline unchanged

Report preview, Word export, and PDF export SHALL continue to use the existing `markdown-it` plugin chain (`VariablePlugin`, `PlanVariablePlugin`, `TablePlugin`, `CaseCardPlugin`, `CardPlugin`, `ImagePlugin`, alignment plugins).

#### Scenario: Preview parity after save

- **WHEN** a template is saved from Milkdown and rendered in `ViewReport.vue`
- **THEN** the rendered HTML MUST match the output produced from the same `template_content` before this change
