## ADDED Requirements

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

## MODIFIED Requirements

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
