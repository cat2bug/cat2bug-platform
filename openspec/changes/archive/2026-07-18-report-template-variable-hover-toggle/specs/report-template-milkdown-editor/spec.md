## MODIFIED Requirements

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
