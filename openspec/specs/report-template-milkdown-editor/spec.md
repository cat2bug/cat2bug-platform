# report-template-milkdown-editor Specification

## Purpose
TBD - 早期归档时 delta 头被原样带入主规范，2026-10-14 修正为主规范结构。

## Requirements

### Requirement: Milkdown editor integration in template edit page

The report template edit page (`EditReportTemplate.vue`) SHALL provide a Milkdown-based WYSIWYG editor component that binds to `template_content` via v-model (or equivalent `content` prop + `update:content` emit).

#### Scenario: Open existing template

- **WHEN** a user opens an existing report template for editing
- **THEN** the Milkdown editor MUST load `template_content` and display recognizable DSL blocks as visual nodes where supported

#### Scenario: Save template

- **WHEN** a user clicks save on the template edit page
- **THEN** the serialized Milkdown document MUST be written to `template_content` via the existing `updateTemplate` API without backend changes

### Requirement: Split edit and preview layout

The editor SHALL support a split layout with editable Milkdown pane and live preview pane, matching the current `Cat2BugMarkdown` dual-pane behavior.

#### Scenario: Toggle preview pane

- **WHEN** a user toggles the preview visibility control
- **THEN** the editor MUST switch between split view and single edit view without losing content

#### Scenario: Preview uses markdown-it

- **WHEN** the preview pane is visible
- **THEN** preview rendering MUST use the same `markdown-it` plugin configuration as `ViewReport.vue` for WYSIWYG fidelity with final report output

### Requirement: Toolbar parity with MarkdownTools

The editor SHALL expose a toolbar that covers the capabilities currently provided by `MarkdownTools.js`: typography, alignment, project/case/defect/plan inserts, and plan statistics table insertion.

#### Scenario: Insert defect table from toolbar

- **WHEN** a user selects the defect table insert action from the toolbar
- **THEN** the editor MUST insert a visual defect-table block equivalent to the default `$table{api.defect.list[14 columns...]}` token

#### Scenario: Insert case card from toolbar

- **WHEN** a user selects the case card insert action
- **THEN** the editor MUST insert a `$card{api.case.list}` block node

#### Scenario: Insert plan variable from dynamic menu

- **WHEN** project test plans are loaded and the user picks a plan field from the plan submenu
- **THEN** the editor MUST insert a plan-variable chip with the correct `[planNumber]` index

### Requirement: Visual block nodes for high-frequency DSL

The Milkdown editor SHALL provide dedicated node types (with node views) for:

| Priority | DSL construct | Node type |
|----------|---------------|-----------|
| P0 | `${api.project.*}`, `${api.case.total}` | variable unit (production-aligned inline text by default; hover chrome for edit affordances) |
| P0 | `${api.plan.*[n]}` | plan-variable unit (same default/hover rules as ordinary variables) |
| P0 | `$table{api.defect.list[...]}` | defect-table unit (live preview by default; same hover / content↔token rules) |
| P1 | `$card{api.case.list}` | case-card unit (preview by default; same hover / content↔token rules) |
| P1 | `$image[...]{...}` | dynamic-image unit (preview by default; same hover / content↔token rules) |

`${}` / `$table` / `$card` / `$image` units MUST NOT use always-visible chip or block borders as the default resting style. Default resting display MUST match production report rendering; editor chrome for these units is defined by the `report-template-variable-preview` capability. Plan-statistics and other raw preserved blocks MAY retain always-visible read-only block chrome.

#### Scenario: Configure defect table columns visually

- **WHEN** a user selects a defect-table block and opens its configuration UI
- **THEN** the user MUST be able to add, remove, reorder columns and set optional column labels without typing raw DSL

#### Scenario: Defect table serializes to DSL

- **WHEN** a user configures a defect-table block and saves
- **THEN** the block MUST serialize to a valid `$table{api.defect.list[...]}` token

#### Scenario: Dollar-token resting style is not persistent chrome

- **WHEN** a `${}` / `$table` / `$card` / `$image` unit is shown in the visual pane and the pointer is not hovering it
- **THEN** the unit MUST appear without persistent chip/block border chrome

### Requirement: Phase-gated delivery

Implementation SHALL follow phased delivery: Phase 0 (defect-table + basic variables), Phase 1 (case card + image + plan chips), Phase 2 (plan mega-table / multimd fallback). Earlier phases MUST NOT block shipping once acceptance criteria pass.

#### Scenario: Phase 0 minimum shippable editor

- **WHEN** Phase 0 is complete
- **THEN** users MUST be able to create/edit templates with headings, paragraphs, project variables, and defect tables via Milkdown, with successful save and preview

### Requirement: Coexistence with legacy editor during migration

Until full parity is achieved, the system MAY retain `Cat2BugMarkdown` behind a feature flag or fallback path for edge templates.

#### Scenario: Fallback to source editor

- **WHEN** a template contains constructs the Milkdown editor cannot safely edit (e.g. complex multimd merged tables)
- **THEN** the user MUST be able to switch to or fall back to the legacy CodeMirror editor without data loss

### Requirement: Automated test coverage

The editor implementation SHALL include unit tests for DSL parse/serialize round-trip and at least one Playwright e2e covering template edit → insert defect table → save → preview.

#### Scenario: Unit test round-trip suite

- **WHEN** CI runs unit tests for report template DSL
- **THEN** tests MUST cover at minimum: project variable, plan variable, defect table (default + alias columns), case card, image token

#### Scenario: E2E template edit flow

- **WHEN** Playwright e2e runs the report template scenario
- **THEN** it MUST verify that saved `template_content` renders a defect table in preview
