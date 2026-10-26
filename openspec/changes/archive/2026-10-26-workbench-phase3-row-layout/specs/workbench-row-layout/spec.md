## ADDED Requirements

### Requirement: Workbench shell with work and board tabs

The project workbench module SHALL present dual tabs **工作台** (`work`) and **看板** (`board`). The work tab uses v3 row layout (`tabs.work.rows`). The board tab shows project-level statistic widgets from `tabs.board.widgets` (checkbox edit). Board widgets MUST NOT be required on the work surface unless placed in a work row.

#### Scenario: Open project workbench

- **WHEN** a project member opens the project landing or dashboard module path
- **THEN** both work and board tab controls are presented
- **THEN** the work tab is the default selected surface

#### Scenario: Board shows project stats

- **WHEN** a member opens the board tab after applying a role preset that includes board widgets
- **THEN** those project-level statistic widgets are available on the board tab

### Requirement: Version 3 row-column configuration

The system SHALL persist workbench configuration keyed by `(userId, projectId)` with `version` equal to `3`. Each tab entry that holds layout (at least `tabs.work`) MUST store an ordered `rows` array. Each row MUST contain an ordered `cols` array. Each column MUST store a relative `ratio` (positive number) and MAY store a widget instance `{ instanceId, type, props }` or be empty. The system MUST treat `instanceId` as the stable identity of a placed widget so that the same `type` MAY appear more than once.

#### Scenario: Save row layout

- **WHEN** a member saves an edited workbench with two rows and mixed column ratios
- **THEN** the stored config has `version: 3` and `tabs.work.rows` reflecting that structure
- **THEN** each placed widget has a unique `instanceId` within the tab

#### Scenario: Unsupported older config

- **WHEN** the client loads a stored config with `version` less than `3` or without `rows`
- **THEN** the client applies the current role preset as a v3 row layout
- **THEN** the client does not attempt field-level migration of free-grid `layout` items

### Requirement: Column count and width ratios

In edit mode, the system SHALL allow the member to set the number of columns for each row and the width ratios of those columns. A single row MUST support at most **4** columns. Column widths MUST use a **24-unit grid**: each column's `ratio` is a positive integer span, spans in a row MUST sum to **24**, and each column MUST be at least **2** spans. The editor MUST expose ratios via a **select dropdown** of valid presets (not free-text input). Dragging a column divider MUST snap to integer spans while preserving the 24-sum invariant. Ratios MUST be normalized on load/save so invalid legacy relative values are mapped onto the 24-grid.

#### Scenario: Set three columns with custom ratio

- **WHEN** a member sets a row to 3 columns with ratio `12:6:6` (24-grid) and saves
- **THEN** the first column occupies half the row width and the other two occupy one quarter each (within layout tolerance)

#### Scenario: Reject more than four columns

- **WHEN** a member attempts to set a row to more than 4 columns
- **THEN** the editor refuses the change or requires splitting into multiple rows
- **THEN** the saved config never contains a row with more than 4 columns

### Requirement: Height modes for rows and columns

The system SHALL support height mode `content`, `row`, and `fixed` (with pixel value) on rows. A column MAY override the row height mode. `content` MUST size from widget content; `row` MUST align sibling columns to the tallest content height in that row; `fixed` MUST use the specified height with overflow handled inside the column.

#### Scenario: Fixed row height

- **WHEN** a row uses `height.mode = fixed` with a positive `px` value
- **THEN** the row viewport height matches that value
- **THEN** overflowing widget content scrolls or clips inside the column without expanding the row

#### Scenario: Column overrides row height

- **WHEN** a row uses `row` height mode and one column sets `content` height mode
- **THEN** that column sizes to its content while siblings follow the row rule as specified by the renderer’s documented behavior for mixed modes

### Requirement: Place widgets by drag and by picker

In edit mode, the system SHALL allow placing a widget into an empty column both by dragging from the widget catalog and by choosing a widget type through an in-slot picker. The system SHALL allow moving or swapping widgets between columns by drag. Removing a widget MUST leave an empty column slot unless the member deletes the column or row.

#### Scenario: Drag from catalog

- **WHEN** a member drags a catalog widget onto an empty column and saves
- **THEN** that column’s widget `type` matches the catalog entry
- **THEN** a new `instanceId` is assigned

#### Scenario: Pick from slot

- **WHEN** a member opens the empty-slot picker, selects a widget type, and saves
- **THEN** the column contains that widget type with a new `instanceId`

#### Scenario: Swap by drag

- **WHEN** a member drags a widget from one column onto another occupied column
- **THEN** the two widget instances swap columns (or the editor completes an equivalent explicit swap)
- **THEN** both `instanceId` values are preserved

### Requirement: Role presets emit row layouts

Presets `tester`, `developer`, `manager`, and `blank` MUST define default `tabs.work.rows` for `version: 3`. Applying a preset MUST replace the current workbench rows for that user and project after confirmation.

#### Scenario: Apply manager preset

- **WHEN** a member confirms applying the `manager` preset
- **THEN** stored config `preset` is `manager` and `version` is `3`
- **THEN** `tabs.work.rows` matches the manager default row structure

### Requirement: View mode renders rows

When not editing, the workbench MUST render `tabs.work.rows` in order, laying out each row’s columns by ratio. On narrow viewports the system MUST stack columns vertically within a row while preserving row order.

#### Scenario: Desktop render

- **WHEN** a member views a saved two-column row on a wide viewport
- **THEN** both columns appear side by side with widths matching their ratios

#### Scenario: Narrow viewport stack

- **WHEN** a member views the same row on a narrow viewport
- **THEN** columns stack vertically in column order
- **THEN** row order is unchanged

### Requirement: Layout editor targets tab id

The row layout editor MUST accept a tab identifier and edit `tabs.<tabId>.rows`. For this change the only implemented tab id is `work`, but the editor MUST NOT hard-require that future tab ids cannot reuse the same rows schema.

#### Scenario: Edit work tab

- **WHEN** a member enters workbench edit mode
- **THEN** changes apply to `tabs.work.rows`
- **THEN** saving persists those rows under the `work` tab key
