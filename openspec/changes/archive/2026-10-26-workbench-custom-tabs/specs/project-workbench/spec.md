## MODIFIED Requirements

### Requirement: Project landing shows workbench shell with tabs

Within a project, the module formerly presented as a fixed dashboard SHALL present a **workbench** shell with a fixed **工作台** tab, optional custom tabs (private and project-shared), and an add control, as specified by `workbench-custom-tabs`. The browser path MAY remain `/{teamKey}/{projectKey}` or `/{teamKey}/{projectKey}/dashboard`. The shell MUST NOT present a **看板** tab.

#### Scenario: Open work tab by default

- **WHEN** a project member opens the project landing or dashboard module and has no valid locally cached custom tab selection
- **THEN** the workbench shell is shown
- **THEN** the **工作台** tab is selected by default
- **THEN** the personal work layout rows are rendered

#### Scenario: No board tab

- **WHEN** the member inspects the workbench tab bar
- **THEN** there is no **看板** tab
- **THEN** former dashboard statistics remain available only as catalog widgets on editable layouts

### Requirement: Per-user per-project layout persistence

The system SHALL persist the personal **工作台** layout configuration keyed by `(userId, projectId)`, including preset id and v3 row layout for that personal surface. Configuration MUST sync across sessions for that user and project. Custom tab layouts MUST persist separately per `workbench-custom-tabs` and MUST NOT be stored only inside another user's personal workbench blob.

#### Scenario: Save layout after edit

- **WHEN** a member finishes editing the personal **工作台** layout and saves
- **THEN** a subsequent visit to the same project by the same user restores that personal layout

#### Scenario: Missing layout uses role preset

- **WHEN** a member has no saved personal layout for the project
- **THEN** the system applies a default role-based preset (`manager` / `tester` / `developer` / `outsider`) without requiring a prior save

### Requirement: Role presets

The system SHALL provide role-oriented presets at least `tester`, `developer`, `manager`, and `outsider` for personal **工作台** defaults, plus create-time `document` and edit-time `custom` as specified by `workbench-row-layout`. Applying a preset from edit mode MUST replace the current surface layout after explicit confirmation. Presets MUST define v3 rows and MUST NOT depend on a board widget list.

#### Scenario: Apply tester preset

- **WHEN** a member confirms applying the `tester` preset on the personal workbench
- **THEN** the work surface includes at least my-defects and today-plan-items style widgets (per product preset table)
- **THEN** the previous custom rows for that personal surface are replaced

### Requirement: Personal statistics on work tab

The personal **工作台** SHALL support personal statistics widgets scoped to the current authenticated user in the current project (for example: my open defect counts, my handled/closed trend, my plan-item progress). Personal widgets MUST NOT show project-wide aggregates unless the widget is explicitly documented as project-scoped. Project-scoped statistic widgets MAY be placed on the personal workbench or on custom tabs via the catalog; they MUST NOT require a board tab.

#### Scenario: Tester preset shows personal KPI

- **WHEN** a member uses the `tester` preset with no custom personal layout
- **THEN** the work tab includes at least one personal KPI or personal trend widget whose data is filtered to the current user

#### Scenario: Personal trend excludes others' volume

- **WHEN** the member views a personal defect-handling trend widget
- **THEN** the series reflect only actions attributed to the current user in that project

## REMOVED Requirements

### Requirement: Light customization in phase one

**Reason**: Superseded by v3 row layout editing (`workbench-row-layout`) and custom tabs; phase-one show/hide-only editing is no longer the product path.
**Migration**: Use row layout editor; board hide/show scenarios no longer apply.

### Requirement: Board widgets are decomposable statistics

**Reason**: Board tab removed; former board statistics become catalog widgets on work/custom surfaces.
**Migration**: Place statistic widgets from the catalog while editing any editable surface.

### Requirement: Phase-two free grid layout

**Reason**: Free-grid `version >= 2` path was already superseded by v3 rows; keeping it conflicts with the current workbench model.
**Migration**: Clients use v3 rows only; ignore legacy `layout` fields if present.
