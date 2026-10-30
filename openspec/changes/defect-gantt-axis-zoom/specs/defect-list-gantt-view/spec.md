## MODIFIED Requirements

### Requirement: Gantt layout with scheduled and unscheduled regions

The Gantt view SHALL show an upper region for defects that have both plan start and plan end (or a coherent plan interval as implemented) and a lower unscheduled pool for defects lacking plan times. The upper region MUST use a left row list and a right time axis that scroll vertically in sync. The time axis MUST support day, week, and month scales, show a today marker, and shade weekend days. Column width for the active scale MUST be adjustable via header column-seam drag as specified in `defect-gantt-axis-zoom`, including automatic scale changes at width thresholds; the toolbar scale control MUST remain available and stay consistent with drag-driven scale changes.

#### Scenario: Unscheduled defects appear in the pool

- **WHEN** a defect in the current filter has no plan start/end suitable for a bar
- **THEN** it appears in the lower unscheduled pool and not as a bar in the upper time axis

#### Scenario: Change time scale

- **WHEN** the user selects week scale
- **THEN** the time axis headers and bar positions update to the week scale while keeping the same defects and grouping

#### Scenario: Drag column width can change scale

- **WHEN** the user shrinks day columns past the configured narrow threshold
- **THEN** the time axis switches to week scale while keeping the same defects and grouping
