## MODIFIED Requirements

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
