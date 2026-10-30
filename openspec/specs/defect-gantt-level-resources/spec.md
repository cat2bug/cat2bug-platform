# defect-gantt-level-resources Specification

## Purpose
TBD - created by archiving change defect-gantt-level-resources. Update Purpose after archive.
## Requirements
### Requirement: One-shot level candidate set

The system SHALL compute a one-shot auto-arrange candidate set as defects that are (1) not closed, (2) scheduled with plan start and plan end, and (3) editable under auto-level rules. Closed and unscheduled defects MUST NOT be candidates. Defects the user cannot edit MUST NOT be moved.

#### Scenario: Closed excluded

- **WHEN** a defect is closed and has plan dates
- **THEN** it is not included in the one-shot candidate set

#### Scenario: Unscheduled excluded

- **WHEN** a defect is not closed but lacks plan start or plan end
- **THEN** it is not included in the one-shot candidate set

### Requirement: One-shot level edit permission

The system SHALL allow users with project role `project.create-by` or `project.admin` (and defect edit permission) to level all non-closed scheduled defects. Other users with defect edit permission SHALL only level defects where they are among `handleBy`. Users without defect edit permission MUST NOT start one-shot auto-arrange.

#### Scenario: Admin levels all open scheduled

- **WHEN** a project admin with defect edit permission runs auto-arrange
- **THEN** every non-closed scheduled defect in the project MAY receive proposed plan dates

#### Scenario: Member levels only own handled

- **WHEN** a non-admin member with defect edit permission runs auto-arrange
- **THEN** only non-closed scheduled defects whose handlers include that member MAY receive proposed plan dates

#### Scenario: No edit permission

- **WHEN** the user lacks `system:defect:edit`
- **THEN** the auto-arrange button is unavailable or disabled

### Requirement: Air obstacles for one-shot level

When computing leveled plan dates, the system MUST NOT treat non-candidate scheduled bars (closed or non-editable) as resource occupancy that blocks placement. Overlap with such bars MAY remain after apply. Among same-lane candidates, the system MUST NOT schedule overlapping plan intervals.

#### Scenario: Ignore non-editable overlap

- **WHEN** a candidate would overlap a non-editable scheduled defect after ASAP placement
- **THEN** the algorithm still places the candidate at the ASAP slot among candidates and does not delay solely to avoid that non-editable bar

### Requirement: Handler-lane ASAP leveling with FS

The system SHALL level candidates by first handler id with lane capacity one. **Working-day** plan duration MUST be preserved under the project work calendar (unless ignore-calendar is enabled for the run). Placement MUST be ASAP and MAY move bars earlier. Explicit FS plan links MUST be respected using next-working-day semantics. Same-lane candidates MUST be packed without avoidable idle **working-day** gaps. Same-layer order SHOULD follow Gantt list top-to-bottom (`visualOrder`) when provided. The system MUST NOT reverse FS order among linked defects.

#### Scenario: Same handler pack without gap

- **WHEN** two editable non-closed defects share the same first handler, have no FS between them, and currently overlap or leave a working-day gap
- **THEN** after leveling they are sequential without overlap and without an avoidable same-lane working-day gap

#### Scenario: FS order preserved

- **WHEN** an FS link A→B exists and both are candidates
- **THEN** B’s proposed start is not before the next working day after A’s proposed end per product day semantics under the work calendar

#### Scenario: Visual order prefers upper row earlier

- **WHEN** two same-lane candidates have no FS and a visualOrder lists upper before lower
- **THEN** the upper row’s proposed start is not after the lower row’s proposed start

### Requirement: Preview then confirm

The system SHALL show a preview of proposed plan dates before persisting. Cancel MUST discard without writing. Confirm MUST persist only changed plan start/end via defect update. Preview input MUST load a complete candidate-oriented dataset for the project, not only currently paged Gantt rows.

#### Scenario: Cancel preview

- **WHEN** the user opens auto-arrange preview and cancels
- **THEN** no defect plan dates are changed

#### Scenario: Confirm applies changes

- **WHEN** the user confirms a non-empty auto-arrange preview
- **THEN** defects whose proposed dates differ are updated with the proposed plan start and plan end

#### Scenario: Empty candidates

- **WHEN** auto-arrange is invoked but the candidate set is empty
- **THEN** the system informs the user and does not enter a confirmable preview

### Requirement: Coexist with drag-yield toggle

One-shot auto-arrange MUST remain available regardless of whether the layers drag-yield checkbox is checked. Completing or canceling one-shot MUST NOT require changing that checkbox. Drag-yield behavior remains as defined by the drag-push auto-level capability and is out of scope for rewriting here except naming in the Gantt view specs.

#### Scenario: Button works with toggle off

- **WHEN** drag-yield is unchecked and the user runs auto-arrange with a non-empty candidate set that needs changes
- **THEN** preview and confirm still proceed

### Requirement: One-shot level respects work calendar

When computing one-shot auto-arrange proposals, the system MUST place candidates only on working days defined by the project work calendar, preserve each candidate’s **working-day** duration (not raw calendar-day span), pack same-lane candidates without avoidable **working-day** idle gaps, and advance FS successors using next-working-day semantics. Non-working days MUST NOT be treated as available capacity for placement.

#### Scenario: Weekend skipped when packing

- **WHEN** two same-lane candidates would otherwise leave only a Saturday–Sunday gap between them under a Mon–Fri calendar
- **THEN** after leveling they abut on working days without counting that weekend as an avoidable work gap that must be filled with work

#### Scenario: Duration preserved in working days

- **WHEN** a candidate spans three working days before leveling
- **THEN** its proposed interval still covers three working days under the calendar even if the natural-day span changes

### Requirement: Optional ignore calendar for one-shot preview

The one-shot auto-arrange preview MUST allow an optional control to ignore the work calendar for **that preview only**, falling back to natural-day leveling. Ignoring the calendar MUST NOT persist as a project calendar change. Default MUST be to respect the calendar.

#### Scenario: Ignore calendar for one run

- **WHEN** the user enables ignore-calendar on auto-arrange preview and confirms
- **THEN** proposed dates follow natural-day leveling for that apply and the stored project calendar exceptions are unchanged

#### Scenario: Default respects calendar

- **WHEN** the user runs auto-arrange without enabling ignore-calendar
- **THEN** proposals respect the project work calendar

