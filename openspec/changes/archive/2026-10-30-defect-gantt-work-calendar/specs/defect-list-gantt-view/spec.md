## ADDED Requirements

### Requirement: Gantt uses project work calendar for schedule math

While the Gantt content view is active, plan duration display, unscheduled drop default duration, horizontal shift that preserves duration, and edge resize minimum duration MUST use **working days** from the current project work calendar (see `project-work-calendar`). Dropping onto a non-working day MUST align the plan start to the next working day (or equivalent product alignment) before applying working-day duration. Existing stored plan start/end MUST NOT be rewritten solely because the calendar was loaded. FS earliest-start-after-predecessor MUST use the next working day per product end-day semantics under the same calendar.

#### Scenario: Unscheduled drop spans working days

- **WHEN** the user drops an unscheduled defect onto a working day with default duration of one working day
- **THEN** plan start is that working day and plan end is computed so the inclusive working-day count is one under the project calendar

#### Scenario: Drop on holiday aligns start

- **WHEN** the user drops an unscheduled defect onto a date that is a holiday under the project calendar
- **THEN** the resulting plan start is the next working day and duration uses working days

#### Scenario: Stored bars not auto-rewritten

- **WHEN** the Gantt loads a defect that already has plan start and plan end
- **THEN** those timestamps are unchanged until the user drags, resizes, auto-arranges, or otherwise edits them

### Requirement: Axis shades holidays and makeup days

On the day scale, the time axis MUST shade non-working days according to the project work calendar, distinguishing ordinary work-week weekends/off-days from `holiday` exceptions where practical, and MUST NOT paint makeup `workday` exceptions as weekend/off shading.

#### Scenario: Holiday shaded on day scale

- **WHEN** the axis is on day scale and a midweek date is a `holiday` exception
- **THEN** that column uses non-working shading

#### Scenario: Makeup Sunday not weekend-shaded

- **WHEN** a Sunday is a `workday` makeup exception
- **THEN** that column is not treated as a non-working weekend shade

## MODIFIED Requirements

### Requirement: Drag schedule and field updates

Authorized users MUST be able to: (1) drag an unscheduled defect onto the time axis to set plan start/end with default duration of **one working day** at the (aligned) drop date under the project work calendar; (2) drag a bar horizontally to shift the interval without changing **working-day** duration; (3) drag bar edges to change start or end with a minimum duration of **one working day** on the day scale; (4) drop onto a handler group row to set `handleBy` to only that handler and update plan times as implied by the drop; (5) drop onto a deliverable group row to set `moduleId` to that module without changing handlers; (6) drop onto empty axis space to change only plan times. Updates MUST persist through the existing defect update path and respect edit permissions. Double-click or equivalent on a bar/row MUST open the existing defect detail surface. Schedule math MUST respect the project work calendar as specified in the Gantt work-calendar requirements.

#### Scenario: Unscheduled drop creates one-day plan

- **WHEN** the user drags an unscheduled defect onto a working calendar day on the axis
- **THEN** the defect gains plan start on that day (or the aligned next working day if required) and plan end such that the working-day duration is one, and moves to the scheduled region

#### Scenario: Drop on handler row replaces handlers

- **WHEN** grouping is by handler and the user drops a defect onto handler A’s group row
- **THEN** the defect’s handlers become only A and its plan times update according to the drop position under the work calendar

#### Scenario: Drop on deliverable row changes module only

- **WHEN** grouping is by deliverable and the user drops a defect onto module M’s group row
- **THEN** the defect’s module becomes M, handlers are unchanged, and plan times update according to the drop position under the work calendar

#### Scenario: Empty axis drop keeps handlers and module

- **WHEN** the user drops a bar onto empty time-axis space (not on a group row)
- **THEN** only plan start/end change and handlers and module stay the same
