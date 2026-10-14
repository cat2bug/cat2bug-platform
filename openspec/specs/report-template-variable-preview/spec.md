# report-template-variable-preview Specification

## Purpose
TBD - 早期归档时 delta 头被原样带入主规范，2026-10-14 修正为主规范结构。

## Requirements

### Requirement: Default variable display matches production markdown

In the report template Milkdown visual pane, built-in DSL variable units SHALL render by default (pointer not hovering) without dashed border, two-state control, or title row. Variable units include:

- `${…}` ordinary variables (`cat2bug_variable`)
- plan `${api.plan…[n]}` (`cat2bug_plan_variable`)
- `$table{…}` (`cat2bug_defect_table`)
- `$card{…}` (`cat2bug_case_card`)
- `$image…{…}` (`cat2bug_dynamic_image`)

In content mode, `${}` units MUST show the resolved preview text; `$table` / `$card` / `$image` MUST show their live preview content. Empty or error states MUST use neutral placeholders consistent with current preview behavior. The resting visual MUST be consistent with production report rendering (`ViewReport` / markdown-it plugins).

#### Scenario: Inline variable content mode looks like surrounding prose

- **WHEN** a template contains `${api.project.projectName}` and preview data resolves to a project name
- **THEN** the visual pane MUST show that name as inline text without chip chrome while the pointer is not hovering the variable

#### Scenario: Table content mode shows preview without chrome

- **WHEN** a template contains `$table{api.defect.list[…]}` in content mode and the pointer is not hovering it
- **THEN** the visual pane MUST show the live table preview without dashed border, toggle, or title

#### Scenario: Variable mode shows raw token without chrome when not hovered

- **WHEN** a variable unit is in variable mode and the pointer is not hovering it
- **THEN** the visual pane MUST show the raw token string (e.g. `${api.project.projectName}` or `$card{api.case.list}`) without border, toggle, or title

### Requirement: Hover reveals editor chrome with title and two-state control

While the pointer hovers a variable unit, the editor SHALL reveal a dashed border, a top-left two-state toggle control, and a title row as a floating overlay that MUST NOT shift the underlying content position (no layout change from padding/border/display). The two-state control MUST switch the unit between content mode (resolved/preview) and variable mode (raw token). The control SHALL occupy the former decorative icon position; there MUST NOT be a separate decorative icon or a right-side expand chevron.

#### Scenario: Hover shows chrome on dollar-brace units

- **WHEN** the user moves the pointer over a `$table` / `$card` / `$image` / `${}` unit
- **THEN** the unit MUST show a dashed border, the top-left two-state control, and a title row

#### Scenario: Toggle to variable mode

- **WHEN** the user activates the two-state control while in content mode
- **THEN** the unit body MUST display the original raw token and the control MUST reflect variable mode

#### Scenario: Toggle to content mode

- **WHEN** the user activates the two-state control while in variable mode
- **THEN** the unit body MUST display the resolved preview value or live preview and the control MUST reflect content mode

### Requirement: Hover title identifies the variable

While chrome is visible, the title row SHALL show a human-readable label when one is available (aligned with toolbar insert labels where possible); otherwise it SHALL fall back to a short path segment or fixed block label. The title MUST NOT replace the variable-mode body text, which remains the raw token.

#### Scenario: Known project variable title

- **WHEN** the user hovers a `${api.project.projectName}` unit
- **THEN** the title row MUST show a label equivalent to the project-name insert label (e.g. localized「项目名称」)

#### Scenario: Known table title

- **WHEN** the user hovers a `$table{…}` unit
- **THEN** the title row MUST show a label equivalent to the defect-table insert label (e.g. localized「缺陷表格」)

#### Scenario: Unknown path fallback

- **WHEN** the user hovers a `${}` variable whose path has no label mapping
- **THEN** the title row MUST show a non-empty fallback derived from the path (e.g. last segment)

### Requirement: Toolbar expands or collapses all variables

The report template editor toolbar SHALL provide a checkbox-like control that sets all variable units (`${}` / plan `${}` / `$table` / `$card` / `$image`) to content mode (expand all) or variable mode (collapse all). The default state on editor open MUST be expand-all (content mode). Applying the toolbar control MUST override any per-unit local mode. Leaving hover MUST NOT change the content/variable mode—only chrome visibility.

#### Scenario: Default all content mode

- **WHEN** the user opens the template editor
- **THEN** all variable units MUST start in content mode and the toolbar control MUST reflect the expanded state

#### Scenario: Collapse all via toolbar

- **WHEN** the user toggles the toolbar control to collapsed
- **THEN** every variable unit in the visual pane MUST switch to variable mode (raw token body)

#### Scenario: Expand all via toolbar

- **WHEN** the user toggles the toolbar control to expanded
- **THEN** every variable unit MUST switch to content mode

### Requirement: Plan-statistics renders as a Markdown table preview (not a single variable unit)

The hover chrome and two-state toggle SHALL apply to independent `${}` / `$table` / `$card` / `$image` nodes. A plan-statistics raw preserved block MUST NOT become a single collapsible variable unit (no whole-block token mode). In the visual pane it MUST render as an HTML table parsed from its Markdown pipe table; cell `${…}` / plan-indexed variables MUST follow the toolbar expand/collapse content vs raw-token display. Other raw preserved blocks (e.g. multimd-table) MAY keep read-only block chrome. Source mode and serialized `template_content` MUST remain unchanged by expand/collapse state.

#### Scenario: Plan-statistics shows table with in-cell variables

- **WHEN** the visual pane displays a plan-statistics preserved block
- **THEN** the editor MUST show a table layout (not a raw Markdown pre), and cell variables MUST show resolved preview text when expanded and raw `${…}` tokens when collapsed

#### Scenario: Plan-statistics is not a whole-block variable unit

- **WHEN** the user toggles the toolbar expand/collapse control
- **THEN** the plan-statistics block MUST NOT switch to a single raw-token body for the entire table; only in-cell variables change display mode

#### Scenario: Save ignores expand state

- **WHEN** the user collapses all variables and saves the template
- **THEN** saved `template_content` MUST still contain the original `${…}` / `$table` / `$card` / `$image` tokens without embedding UI expand state
