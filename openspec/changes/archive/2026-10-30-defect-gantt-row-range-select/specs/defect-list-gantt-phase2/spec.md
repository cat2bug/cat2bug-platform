## MODIFIED Requirements

### Requirement: Multi-select and batch shift

The Gantt view SHALL always allow box-selecting multiple scheduled bars on the axis blank area (no separate marquee-mode toggle). Selected rows MUST show full-row selected state on both the left label column and the axis row. Authorized users MUST be able to batch-shift the selected defects’ plan intervals by dragging any selected bar horizontally by the same day delta while preserving each defect’s duration (under the project work calendar as elsewhere specified). The system MUST persist updates through the existing defect update authorization path and report partial failures if some updates fail. Clicking empty axis without a meaningful drag MUST clear the multi-selection.

For **scheduled** defects (left row labels and right bars), pointer selection MUST follow Finder/Excel-style modifiers and MUST be symmetric left/right:

- No modifier: replace the selection with that defect and set the selection anchor to it.
- Ctrl (Windows/Linux) or ⌘ (macOS): toggle that defect in or out of the selection and set the anchor to it.
- Shift: select the inclusive range of defects from the selection anchor through the clicked defect along the **visible flattened scheduled row order** (top-to-bottom across expanded groups; collapsed groups’ rows excluded; multi-handler mirror rows dedupe by `defectId`), replacing the selection; the anchor MUST NOT move.
- Ctrl/⌘+Shift: compute the same inclusive range and union it into the existing selection; the anchor MUST NOT move.

If there is no selection anchor when Shift is used, the click MUST behave as a single selection (set anchor). The unscheduled pool MUST NOT participate in multi-select or range selection for this requirement (plain single-select only). Modifier-assisted selection on the left list MUST NOT force scrolling the axis to the bar (plain single-select MAY keep focus/scroll behavior).

#### Scenario: Batch shift selection by drag

- **WHEN** the user box-selects three scheduled bars and drags one of them by +2 day columns
- **THEN** each selected defect’s plan start and end move forward by two days and durations stay the same

#### Scenario: Single bar when not multi-selected

- **WHEN** only one bar is selected (or none of a multi-set is under the pointer as a batch)
- **THEN** dragging that bar moves only that bar as in phase one

#### Scenario: Left list Ctrl toggles selection

- **WHEN** the user Ctrl/⌘-clicks a scheduled row label that is not selected
- **THEN** that defect is added to the multi-selection and the row shows selected state without requiring a bar marquee

#### Scenario: Shift range across groups

- **WHEN** the selection anchor is defect A in group 1 and the user Shift-clicks defect B in a later expanded group 2
- **THEN** all unique scheduled defects from A through B in visible flattened order (including intervening groups) become the selection

#### Scenario: Unscheduled pool stays single-select

- **WHEN** the user Ctrl/⌘-clicks or Shift-clicks a defect in the unscheduled pool
- **THEN** the pool click does not build a multi-selection range for pooling; selection behaves as single-select for that pool interaction
