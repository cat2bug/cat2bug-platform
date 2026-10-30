# defect-list-gantt-phase2 Specification

## Purpose

缺陷甘特二期：计划依赖与关联线、关键路径高亮、处理人资源负荷轨、计划基线对比、多选批量平移改期。
## Requirements
### Requirement: Plan dependencies between defects

The system SHALL allow authorized users to create, list, and delete plan dependency links between two defects in the same project. Phase-two link type MUST be finish-to-start (`FS`): the successor MUST NOT be planned to start before the predecessor’s plan end in validation messaging (hard block on create if it would form a cycle; schedule-violation on move MAY warn). Self-links and cross-project links MUST be rejected. Links MUST persist independently of the Gantt UI.

#### Scenario: Create FS link

- **WHEN** an editor creates an FS link from defect A to defect B in the same project and no cycle would result
- **THEN** the link is stored and subsequent Gantt loads for that project can retrieve it

#### Scenario: Reject cycle

- **WHEN** an editor attempts to add a link that would create a directed cycle
- **THEN** the operation fails and no link is stored

#### Scenario: Delete link

- **WHEN** an editor deletes an existing plan link
- **THEN** the link no longer appears in list responses or on the Gantt dependency layer

### Requirement: Gantt draws dependency lines

While the Gantt dependency (link-line) layer is enabled, the Gantt view MUST draw dashed visual connectors for FS links whose both endpoints are present in the current scheduled result set. Connectors MUST update when bars move within the view. Links with a missing endpoint in the current filter MUST NOT be drawn as dangling lines into empty space. Authorized editors MUST be able to create FS links by dragging a bar’s tail port to another bar’s head port (or head to tail), and MUST be able to select a connector and delete it with the Delete key.

#### Scenario: Both ends visible

- **WHEN** defects A and B are scheduled and visible and an FS link A→B exists and the dependency layer is on
- **THEN** a dashed connector is shown from A’s bar toward B’s bar

#### Scenario: Drag create FS

- **WHEN** an editor drags from defect A’s tail port to defect B’s head port (or A’s head to B’s tail)
- **THEN** an FS link is created with the correct predecessor → successor orientation

#### Scenario: Delete selected connector

- **WHEN** an editor selects a dependency connector and presses Delete
- **THEN** that plan link is removed

#### Scenario: Filtered-out endpoint

- **WHEN** an FS link exists but one endpoint is not in the current Gantt result set
- **THEN** no connector is drawn for that link on the axis

### Requirement: Critical path highlighting

The Gantt view SHALL offer a critical-path mode that, using only scheduled defects in the current Gantt result set, infers finish-to-start sequencing from plan dates (successor start day ≥ predecessor end day + 1) and highlights bars on a single representative longest-duration chain from early to late (non-critical bars MAY be dimmed). When multiple defects share the same schedule slot on that chain, only one representative bar SHALL be marked critical. Critical-path mode MUST NOT draw dependency-style connectors or arrows; the dependency layer remains the sole toggle for declared link connectors. Critical-path mode MUST NOT require user-declared FS links.

#### Scenario: Highlight start-to-finish chain

- **WHEN** the user enables critical-path mode and the current set has scheduled defects that can chain by plan dates
- **THEN** bars on one representative longest start-to-finish chain are visually distinguished without drawing path connectors

#### Scenario: Parallel same-slot defects

- **WHEN** several defects share the same plan window and would otherwise all lie on a longest path
- **THEN** only one representative among that parallel set is marked critical

#### Scenario: No chainable schedule

- **WHEN** critical-path mode is on but the current set has no scheduled defects (or none with plan dates)
- **THEN** no bars are marked critical

### Requirement: Handler resource load affordance

The Gantt view SHALL provide a resource-load affordance that aggregates planned occupancy by handler over the visible time window for defects in the current result set. Occupancy MUST be derived from plan start/end intervals. When a handler’s load exceeds the product-defined threshold for a day (or equivalent bucket), that bucket MUST be visually warned. When grouped by handler, the affordance SHOULD render as a per-handler load track under the group (normal thin segment / overload thicker segment). The affordance MUST NOT claim actual timesheet hours.

#### Scenario: Overload warning

- **WHEN** resource-load view is enabled and handler H has planned occupancy above the threshold on a visible day
- **THEN** that day/handler cell (or equivalent) shows an overload indication

#### Scenario: Empty window

- **WHEN** resource-load view is enabled and no scheduled defects fall in the visible window for any handler
- **THEN** the load affordance shows empty/zero state without errors

### Requirement: Plan baseline freeze and compare

Project plan admins (`project.create-by` or `project.admin`) SHALL be able to update (freeze/overwrite) the current plan start/end of defects in the project into an active baseline snapshot, and to clear it when the product exposes clear. The Gantt view MUST be able to overlay baseline intervals against current plan bars so differences are visible. Updating again MUST replace the active baseline after confirmation (single-active-baseline). Users who are not project plan admins MUST NOT update or clear the baseline (including via API). Other members MAY view baseline overlay if baseline data exists.

#### Scenario: Update baseline

- **WHEN** a project plan admin confirms updating the baseline for the project
- **THEN** baseline intervals for included defects are stored as the active baseline

#### Scenario: Overlay differs from current plan

- **WHEN** baseline overlay is on and a defect’s current plan dates differ from its baseline dates
- **THEN** the Gantt shows both the current bar and a baseline representation so the shift is visible

#### Scenario: Non-admin freeze rejected

- **WHEN** a non-admin member attempts to freeze or clear the project baseline
- **THEN** the operation is rejected and the active baseline is unchanged

### Requirement: Multi-select and batch shift

The Gantt view SHALL always allow box-selecting multiple scheduled bars on the axis blank area (no separate marquee-mode toggle). Selected rows MUST show full-row selected state on both the left label column and the axis row. Users MUST be able to batch-shift only those selected defects they are allowed to edit under Gantt plan-node rules (project plan admin: all; otherwise only defects whose handlers include the user), by dragging an editable selected bar horizontally by the same day delta while preserving each shifted defect’s duration (under the project work calendar as elsewhere specified). The system MUST persist updates through the existing defect update authorization path, skip or reject unauthorized selected defects, and report partial failures if some updates fail. Clicking empty axis without a meaningful drag MUST clear the multi-selection.

For **scheduled** defects (left row labels and right bars), pointer selection MUST follow Finder/Excel-style modifiers and MUST be symmetric left/right:

- No modifier: replace the selection with that defect and set the selection anchor to it.
- Ctrl (Windows/Linux) or ⌘ (macOS): toggle that defect in or out of the selection and set the anchor to it.
- Shift: select the inclusive range of defects from the selection anchor through the clicked defect along the **visible flattened scheduled row order** (top-to-bottom across expanded groups; collapsed groups’ rows excluded; multi-handler mirror rows dedupe by `defectId`), replacing the selection; the anchor MUST NOT move.
- Ctrl/⌘+Shift: compute the same inclusive range and union it into the existing selection; the anchor MUST NOT move.

If there is no selection anchor when Shift is used, the click MUST behave as a single selection (set anchor). The unscheduled pool MUST NOT participate in multi-select or range selection for this requirement (plain single-select only). Modifier-assisted selection on the left list MUST NOT force scrolling the axis to the bar (plain single-select MAY keep focus/scroll behavior). Selection itself MUST NOT require plan-edit authorization.

#### Scenario: Batch shift selection by drag

- **WHEN** the user box-selects three scheduled bars they can edit and drags one of them by +2 day columns
- **THEN** each selected editable defect’s plan start and end move forward by two days and durations stay the same

#### Scenario: Single bar when not multi-selected

- **WHEN** only one bar is selected (or none of a multi-set is under the pointer as a batch)
- **THEN** dragging that bar moves only that bar as in phase one when the user may edit it

#### Scenario: Left list Ctrl toggles selection

- **WHEN** the user Ctrl/⌘-clicks a scheduled row label that is not selected
- **THEN** that defect is added to the multi-selection and the row shows selected state without requiring a bar marquee

#### Scenario: Shift range across groups

- **WHEN** the selection anchor is defect A in group 1 and the user Shift-clicks defect B in a later expanded group 2
- **THEN** all unique scheduled defects from A through B in visible flattened order (including intervening groups) become the selection

#### Scenario: Unscheduled pool stays single-select

- **WHEN** the user Ctrl/⌘-clicks or Shift-clicks a defect in the unscheduled pool
- **THEN** the pool click does not build a multi-selection range for pooling; selection behaves as single-select for that pool interaction

#### Scenario: Batch shift skips non-editable selection members

- **WHEN** a non-admin’s multi-selection includes a defect they do not handle and they batch-shift
- **THEN** that defect’s plan dates MUST NOT change while editable selected defects MAY shift

### Requirement: Load track display-only; packing via arrange button or drag-yield

The handler resource-load affordance MUST continue to reflect planned occupancy for visualization and overload warning only, with occupancy computed on working days as specified for the work calendar. Enabling the load track MUST NOT by itself rewrite plan dates. Packing same-handler editable bars without avoidable gaps MUST be available through the toolbar auto-arrange action (`defect-gantt-level-resources`) using work-calendar schedule math. Resolving overlaps created while dragging MUST be available through the layers drag-yield checkbox when enabled, also under the work calendar. Neither MUST be a silent side effect of load-track display, dependency drawing, or critical-path mode alone.

#### Scenario: Load track does not rewrite plans

- **WHEN** the user enables the resource-load layer without running auto-arrange or dragging under drag-yield
- **THEN** no defect plan start or plan end is changed solely because the load layer was enabled

#### Scenario: Full pack uses auto-arrange

- **WHEN** the user wants same-handler editable bars packed ASAP without gaps across the project candidate set
- **THEN** they use the toolbar auto-arrange action with preview and confirm

#### Scenario: Drag conflict uses drag-yield

- **WHEN** the user wants overlapping peers to yield while dragging a bar
- **THEN** they enable drag-yield in layers and drag the pinning bar

### Requirement: Load track occupancy ignores non-working days

When the resource-load affordance is enabled, planned occupancy used for load visualization and overload warning MUST count only working days under the project work calendar. Non-working days (work-week off-days and `holiday` exceptions) MUST NOT contribute occupancy toward overload. Makeup `workday` exceptions MUST count as working capacity days.

#### Scenario: Weekend not counted as load

- **WHEN** a bar’s natural span includes Saturday–Sunday under a Mon–Fri calendar and the load track is on
- **THEN** those weekend columns do not add handler occupancy for overload calculation

#### Scenario: Makeup day counts as load capacity day

- **WHEN** a bar covers a Sunday marked as makeup `workday`
- **THEN** that day contributes to load occupancy like other working days

