# defect-module-tree-sidebar Specification

## Purpose
TBD - created by archiving change defect-module-tree-excel-kanban-gantt. Update Purpose after archive.
## Requirements
### Requirement: Excel view has collapsible deliverable filter sidebar

The defect Excel view SHALL provide a true left sidebar deliverable tree (same filter semantics as the table view: selecting a node sets the shared parent `moduleId` query and refreshes the sheet). When expanded, the sidebar title area SHALL expose a hide control to the right of the deliverable label. When collapsed, the sheet top-left corner cell (`th.first-col`) SHALL show a control that expands the sidebar again. Sidebar visibility and width SHALL be persisted in Excel-specific local keys, not the table view keys.

#### Scenario: Expand and filter from Excel corner

- **WHEN** the Excel deliverable sidebar is collapsed and the user clicks the top-left corner expand control
- **THEN** the sidebar expands and remains usable to filter by deliverable

#### Scenario: Hide from Excel sidebar title

- **WHEN** the Excel deliverable sidebar is expanded and the user clicks the hide control next to the deliverable title
- **THEN** the sidebar collapses and the top-left corner expand control is available

#### Scenario: Excel filter updates sheet via shared moduleId

- **WHEN** the user selects a deliverable node in the Excel sidebar
- **THEN** the parent query `moduleId` is updated and the Excel sheet reloads to match that filter

### Requirement: Kanban view has leftmost deliverable filter lane

The defect Kanban view SHALL provide a leftmost deliverable filter pane styled as a kanban lane shell. Selecting a node SHALL only filter cards (MUST NOT regroup cards by deliverable). When expanded, the title area SHALL expose a hide control to the right of the deliverable label. When collapsed, a narrow vertical strip SHALL remain on the far left with a show control at the top. Visibility and width SHALL use Kanban-specific local keys.

#### Scenario: Kanban filter does not regroup lanes

- **WHEN** the user selects a deliverable in the Kanban sidebar
- **THEN** status lanes remain the same four workflow columns and only the cards inside them are filtered by `moduleId`

#### Scenario: Kanban collapsed strip restores sidebar

- **WHEN** the Kanban deliverable sidebar is collapsed and the user clicks the show control on the left strip
- **THEN** the deliverable lane sidebar expands

### Requirement: Gantt view has no separate deliverable filter sidebar

The defect Gantt view MUST NOT add a separate left deliverable filter tree sidebar. Existing Gantt group-by-deliverable behavior MAY continue unchanged.

#### Scenario: Gantt has no module tree toggle

- **WHEN** the active defect content view is Gantt and the user presses Cmd/Ctrl+D
- **THEN** no deliverable sidebar is toggled for Gantt

### Requirement: Cmd-or-Ctrl-D toggles deliverable sidebar in table excel and kanban

The defect page keyboard shortcut Cmd/Ctrl+D SHALL toggle the deliverable sidebar visibility for Table, Excel, and Kanban content views when that view is active.

#### Scenario: Shortcut works outside table

- **WHEN** the active defect content view is Excel or Kanban and the user presses Cmd/Ctrl+D
- **THEN** that view’s deliverable sidebar toggles open or closed

#### Scenario: Table shortcut unchanged

- **WHEN** the active defect content view is Table and the user presses Cmd/Ctrl+D
- **THEN** the table deliverable sidebar toggles as before

### Requirement: Per-view sidebar visibility does not overwrite table cache

Excel and Kanban sidebar expanded/collapsed (and width) preferences SHALL be stored under distinct local storage keys from the table view keys (`defect_tree_module_visible` / `defect_tree_module_width`). Changing one view’s preference MUST NOT change another view’s stored preference.

#### Scenario: Switching views keeps independent visibility

- **WHEN** the user collapses the Excel sidebar, switches to Table where the sidebar was expanded, then returns to Excel
- **THEN** Excel remains collapsed and Table remains expanded per their respective caches

