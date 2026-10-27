## MODIFIED Requirements

### Requirement: Version 3 row-column configuration

The system SHALL persist the personal **工作台** configuration keyed by `(userId, projectId)` with `version` equal to `3` and an ordered `rows` array (under `tabs.work` or an equivalent personal rows root). Custom tab layouts MUST also use `version: 3` row-column structures as specified by `workbench-custom-tabs`. Each row MUST contain an ordered `cols` array. Each column MUST store a relative `ratio` (positive number) and an ordered `widgets` array of zero or more widget instances `{ instanceId, type, props }`. The system MUST treat `instanceId` as the stable identity of a placed widget. The same `type` MAY appear on the same surface in different columns, but MUST NOT appear more than once within a single column's `widgets` array. When loading a legacy column that only stores a singular `widget` field, the client MUST normalize it into a one-element `widgets` array. When loading legacy personal config that still contains `tabs.board`, the client MUST ignore board widgets for shell rendering and MUST NOT require board data to open the workbench.

#### Scenario: Save row layout

- **WHEN** a member saves an edited personal workbench with two rows and mixed column ratios
- **THEN** the stored personal config has `version: 3` and work rows reflecting that structure
- **THEN** each placed widget has a unique `instanceId` within the surface

#### Scenario: Column with stacked widgets

- **WHEN** a member saves a column that contains two widgets of different types stacked top-to-bottom
- **THEN** that column's stored `widgets` array has length 2 in visual top-to-bottom order
- **THEN** each entry has its own `instanceId` and `type`

#### Scenario: Unsupported older config

- **WHEN** the client loads a stored personal config with `version` less than `3` or without `rows`
- **THEN** the client applies the current role preset as a v3 row layout
- **THEN** the client does not attempt field-level migration of free-grid `layout` items

#### Scenario: Legacy board field ignored

- **WHEN** stored personal config still includes `tabs.board.widgets`
- **THEN** the client opens the workbench without a board tab
- **THEN** personal work rows still render

#### Scenario: Legacy singular widget normalized

- **WHEN** the client loads a column that only has a singular `widget` object and no `widgets` array
- **THEN** the runtime column model exposes that instance as a one-element `widgets` list

### Requirement: Height modes for rows and columns

The system SHALL support height mode `content`, `row`, and `fixed` (with pixel value) on rows. A column MAY override the row height mode. Sibling columns in a row MUST share the same rendered row height (except where a column explicitly overrides to `content` as documented below). For row-level `content` mode, the row height MUST equal the maximum of each column's stack height, where a column's stack height is the sum of its placed widgets' content heights plus inter-widget spacing. `row` mode MUST align sibling columns to that shared tallest stack height. `fixed` MUST use the specified height with overflow handled inside each column. Within a column, stacked widgets MUST lay out top-to-bottom in `widgets` order.

#### Scenario: Content height uses max column stack

- **WHEN** a row uses `height.mode = content`, one column has two stacked widgets totaling height H1, and another column has one widget of height H2 where H1 > H2
- **THEN** the row viewport height matches H1 (within layout tolerance)
- **THEN** both columns share that row height

#### Scenario: Fixed row height

- **WHEN** a row uses `height.mode = fixed` with a positive `px` value
- **THEN** the row viewport height matches that value
- **THEN** overflowing stacked widget content scrolls or clips inside the column without expanding the row

#### Scenario: Column overrides row height

- **WHEN** a row uses `row` height mode and one column sets `content` height mode
- **THEN** that column sizes to its own stack content while siblings follow the row rule as specified by the renderer’s documented behavior for mixed modes

### Requirement: Place widgets by drag and by picker

In edit mode, the system SHALL allow placing a widget into a column both by dragging from the widget catalog and by choosing a widget type through a picker (empty-slot picker or an add control on a non-empty column). Placement into a column that already has widgets MUST **append** the new instance to that column's `widgets` array (or insert at an explicit drop index when the member drops between stacked widgets). There is no maximum number of widgets per column other than the same-column `type` uniqueness rule. The system SHALL allow **instance-level** drag to reorder widgets within a column and to move a widget from one column to another (inserting at the drop index or appending when dropping onto the column body). Removing a widget MUST remove only that instance from `widgets`; the column remains as an empty slot when `widgets` becomes empty unless the member deletes the column or row. The picker and catalog drop MUST exclude or reject a `type` already present in the target column: the picker MUST not list those types for that column, and a drag that would create a duplicate `type` in the target column MUST show a warning and MUST NOT change the layout.

#### Scenario: Drag from catalog onto empty column

- **WHEN** a member drags a catalog widget onto an empty column and saves
- **THEN** that column’s `widgets` contains one entry whose `type` matches the catalog entry
- **THEN** a new `instanceId` is assigned

#### Scenario: Append onto occupied column

- **WHEN** a member places a widget type not already in a column that already has one widget
- **THEN** the new instance is appended below the existing widget in `widgets` order
- **THEN** the previous instance is unchanged (not replaced)

#### Scenario: Pick from slot or add control

- **WHEN** a member opens the empty-slot picker or the column add control, selects an allowed widget type, and saves
- **THEN** the column’s `widgets` includes that widget type with a new `instanceId`

#### Scenario: Reorder within column by drag

- **WHEN** a member drags the lower widget in a two-widget column onto the insert position above the upper widget
- **THEN** `widgets` order is updated to match the new visual order
- **THEN** both `instanceId` values are preserved

#### Scenario: Move across columns by drag

- **WHEN** a member drags a widget from column A onto column B where column B does not already contain that widget’s `type`
- **THEN** the instance is removed from column A’s `widgets` and inserted into column B’s `widgets` at the drop index (or appended)
- **THEN** that instance’s `instanceId` is preserved

#### Scenario: Reject duplicate type in same column

- **WHEN** a member attempts to place or move a widget whose `type` already exists in the target column
- **THEN** the editor shows a warning
- **THEN** the layout is left unchanged

#### Scenario: Remove single stacked widget

- **WHEN** a member removes one widget from a column that had two widgets
- **THEN** the remaining widget stays in that column
- **THEN** `widgets` length is 1

## ADDED Requirements

### Requirement: Column widgets render as vertical stack

In both view and edit modes, a column with multiple entries in `widgets` MUST render those widgets as a vertical stack in array order (top to bottom), without requiring nested rows.

#### Scenario: View stacked column

- **WHEN** a member views a saved column whose `widgets` has length greater than 1
- **THEN** widgets appear one above another in `widgets` order within that column’s width
