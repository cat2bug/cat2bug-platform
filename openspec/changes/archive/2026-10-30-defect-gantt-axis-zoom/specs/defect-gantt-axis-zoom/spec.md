## ADDED Requirements

### Requirement: User can drag axis column edges to resize global column width

While the Gantt content view is active, the system SHALL let the user drag a time-axis header column seam (day/week/month unit edge) to change the **global** column width for the current scale. All units on that scale MUST share the same width. Resizing MUST NOT assign independent widths per column. Pointer interaction on the seam MUST NOT start axis marquee selection or bar drag. Column width for each scale MUST be remembered locally and restored with that scale.

#### Scenario: Drag seam widens day columns

- **WHEN** the user is on day scale and drags a day-column seam to the right within the day width limits
- **THEN** every day column becomes wider by the same amount and bars/headers reflow to the new width

#### Scenario: Seam drag does not start marquee

- **WHEN** the user presses and drags on a column seam
- **THEN** no marquee selection rectangle is started on the time axis

### Requirement: Crossing width thresholds switches scale both ways

While resizing column width, if the width crosses the configured **narrow** threshold for the current scale, the system MUST switch to the coarser scale (day → week → month). If the width crosses the configured **wide** threshold, the system MUST switch to the finer scale (month → week → day). Day scale MUST NOT refine further; month scale MUST NOT coarsen further. After a scale switch, the system MUST keep the approximate calendar date that was at the horizontal center of the viewport near the center, update the toolbar scale control to match, show a brief user-visible notice of the new scale, and apply a column width for the new scale that does not immediately re-trigger the opposite threshold.

#### Scenario: Shrink day into week

- **WHEN** the user shrinks day columns past the day narrow threshold
- **THEN** the axis switches to week scale, the toolbar shows week, a brief notice indicates the switch, and the previous viewport-center date remains approximately centered

#### Scenario: Widen week into day

- **WHEN** the user widens week columns past the week wide threshold
- **THEN** the axis switches to day scale, the toolbar shows day, and a brief notice indicates the switch

#### Scenario: Widen month into week

- **WHEN** the user widens month columns past the month wide threshold
- **THEN** the axis switches to week scale with toolbar and notice updated accordingly

### Requirement: Toolbar scale stays consistent with drag-driven scale

Changing scale from the Gantt toolbar MUST use the remembered column width for the selected scale when available (otherwise the scale default width). Changing scale via column-width thresholds MUST update the same scale state the toolbar displays.

#### Scenario: Toolbar select week uses stored week width

- **WHEN** the user previously resized week columns and later selects week from the toolbar
- **THEN** the axis uses the stored week column width rather than always resetting to the built-in default
