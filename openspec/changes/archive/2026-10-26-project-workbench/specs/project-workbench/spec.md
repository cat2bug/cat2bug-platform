## ADDED Requirements

### Requirement: Project landing shows workbench shell with tabs

Within a project, the module formerly presented as a fixed dashboard SHALL present a **workbench** shell with two tabs: **工作台** (work) and **看板** (board). The browser path MAY remain `/{teamKey}/{projectKey}` or `/{teamKey}/{projectKey}/dashboard`.

#### Scenario: Open work tab by default

- **WHEN** a project member opens the project landing or dashboard module
- **THEN** the workbench shell is shown
- **THEN** the **工作台** tab is selected by default
- **THEN** widgets configured for the work tab are rendered in saved order

#### Scenario: Switch to board tab

- **WHEN** the member selects the **看板** tab
- **THEN** widgets configured for the board tab are rendered
- **THEN** former dashboard statistics appear only as individually toggleable widgets, not as one undecomposable page

### Requirement: Per-user per-project layout persistence

The system SHALL persist workbench layout configuration keyed by `(userId, projectId)`, including preset id and ordered widget id lists for work and board tabs. Configuration MUST sync across sessions for that user and project.

#### Scenario: Save layout after edit

- **WHEN** a member finishes editing widget visibility or order and saves
- **THEN** a subsequent visit to the same project by the same user restores that layout

#### Scenario: Missing layout uses role preset

- **WHEN** a member has no saved layout for the project
- **THEN** the system applies a default role-based preset without requiring a prior save

### Requirement: Role presets

The system SHALL provide at least presets `tester`, `developer`, `manager`, and `blank`, each defining default work and board widget lists. Applying a preset MUST replace the current layout for that user and project after explicit confirmation.

#### Scenario: Apply tester preset

- **WHEN** a member confirms applying the `tester` preset
- **THEN** the work tab includes at least my-defects and today-plan-items style widgets (per product preset table)
- **THEN** the previous custom order for that project is replaced

### Requirement: Light customization in phase one

In phase one, layout editing MUST support widget show/hide and reordering within the current tab. Phase one MUST NOT require free-form grid placement or resize.

#### Scenario: Hide a board statistic widget

- **WHEN** a member enters edit mode on the board tab and hides `stat-document`
- **THEN** after save, `stat-document` is not shown on the board tab
- **THEN** other board widgets remain in their relative order

#### Scenario: Reorder work widgets

- **WHEN** a member reorders work-tab widgets and saves
- **THEN** the work tab renders widgets in the new order

### Requirement: Personal statistics on work tab

The work tab SHALL support personal statistics widgets scoped to the current authenticated user in the current project (for example: my open defect counts, my handled/closed trend, my plan-item progress). Personal widgets MUST NOT show project-wide aggregates unless the widget is explicitly documented as project-scoped and placed on the board tab.

#### Scenario: Tester preset shows personal KPI

- **WHEN** a member uses the `tester` preset with no custom layout
- **THEN** the work tab includes at least one personal KPI or personal trend widget whose data is filtered to the current user

#### Scenario: Personal trend excludes others' volume

- **WHEN** the member views a personal defect-handling trend widget
- **THEN** the series reflect only actions attributed to the current user in that project

### Requirement: Actionable work widgets

Work-tab widgets that list defects or plan items SHALL allow common actions without navigating away from the workbench shell (inline controls and/or existing detail drawers/dialogs). Actions MUST respect the member's existing business permissions.

#### Scenario: Close defect from my-defects widget

- **WHEN** a member with close permission triggers close on a defect in the my-defects widget and completes the required dialog
- **THEN** the defect state updates
- **THEN** the widget list refreshes to reflect the change

#### Scenario: Mark plan item passed

- **WHEN** a member with plan-item execute permission marks a today-plan-items row as passed
- **THEN** the plan item result is persisted
- **THEN** the widget reflects the new result

### Requirement: Board widgets are decomposable statistics

Board-tab widgets SHALL include the former dashboard statistic and activity surfaces as separate widget ids that can be independently enabled or disabled. The system MUST NOT present the legacy fixed multi-row dashboard as the only board option.

#### Scenario: Manager preset enables most statistics

- **WHEN** a member applies the `manager` preset
- **THEN** the board tab enables a broad set of former dashboard statistic widgets by default
- **THEN** the member can still hide individual statistic widgets

### Requirement: Phase-two free grid layout

The persisted configuration schema MUST be versioned. Clients that only understand phase-one ordered widget lists MUST ignore unknown fields (including `layout`) and still render. Clients that support free grid MUST:

- Persist optional `tabs.{work|board}.layout` items `{ i, x, y, w, h }` on a 12-column grid when `version >= 2`
- Allow edit mode to drag position and resize widgets
- On viewports below the stack breakpoint, fall back to a single-column order by `(y, x)` without requiring free-form placement

#### Scenario: Phase-one client reads future layout fields

- **WHEN** stored config contains a higher version with grid layout fields
- **THEN** a phase-one client still renders widgets using the ordered id lists and does not fail to load the workbench

#### Scenario: Edit and save free grid

- **WHEN** a member enters edit mode, rearranges or resizes widgets on the 12-column grid, and saves
- **THEN** a subsequent visit restores those `x,y,w,h` values for that user and project

#### Scenario: Small screen stacks grid

- **WHEN** the viewport width is below the stack breakpoint and the saved config has free-grid layout
- **THEN** widgets are shown in a single column ordered by `(y, x)`

### Requirement: Scope is project-local in phase one

Phase one MUST scope workbench layout and content to the current project only. The system MUST NOT require a cross-project personal home workbench for phase one.

#### Scenario: Switching project loads that project's layout

- **WHEN** a member switches from project A to project B
- **THEN** the workbench shows layout and data for project B
- **THEN** project A's layout is unchanged
