# defect-gantt-minimap

## Requirements

### Requirement: Gantt time-axis minimap navigates viewport

While the Gantt content view is active and the minimap is enabled, the system SHALL show a fixed-size floating minimap over the scheduled Gantt main pane. The minimap MUST map only the time-axis scroll container (horizontal and vertical). It MUST NOT control the left row-list scroll or the unscheduled pool. The minimap content MUST show a simplified overview: a neutral background, **one short bar mark per visible scheduled row** (including handler mirror rows) whose horizontal position/width are proportional to that row's plan bar on the axis and whose vertical position follows the same group/row layout as the axis (collapsed groups omitted), colored consistently with the main bar state, and a distinct viewport rectangle (red border or equivalent) whose position and size are proportional to the current axis `scrollLeft`/`scrollTop` and visible client size relative to the axis content size. The viewport rectangle MUST render above the bar marks. Dragging inside the minimap body (including over bar marks or the viewport rectangle) MUST update the axis scroll position only and MUST NOT select defects. Clicking empty minimap area MAY jump the viewport so the click maps to the new scroll center or top-left, as long as behavior is consistent. Minimap pointer interactions MUST NOT start axis marquee selection.

#### Scenario: Viewport rectangle tracks scroll

- **WHEN** the user scrolls the time axis horizontally or vertically while the minimap is visible
- **THEN** the red viewport rectangle moves and resizes to match the new visible region on the axis content

#### Scenario: Drag minimap changes axis scroll

- **WHEN** the user presses and drags on the minimap body or its viewport rectangle
- **THEN** the time-axis scroll position updates to the mapped region and the left list stays vertically in sync with the axis as in existing scroll coupling

#### Scenario: Marquee not started from minimap

- **WHEN** the user presses and drags on the minimap
- **THEN** no marquee selection rectangle is started on the time axis

#### Scenario: Bar marks mirror visible rows

- **WHEN** the minimap is visible and the axis shows multiple scheduled rows including handler mirrors
- **THEN** the minimap shows a state-colored mark for each visible scheduled row (not only the first occurrence of each defect)

#### Scenario: Bar mark does not select defect

- **WHEN** the user presses on a minimap bar mark
- **THEN** the axis scroll may update for navigation and no defect selection change is required from that press

### Requirement: Minimap panel can move and close

The minimap panel MUST include a title (or drag handle) area and a close control. Dragging the title/handle MUST reposition the floating panel within the Gantt main pane; the last position MUST be persisted locally and restored on later visits. Activating close MUST hide the minimap and turn off the minimap layer preference. The default dock position when no saved position exists MUST be the bottom-right of the Gantt scheduled main pane. Panel size MUST be fixed for this change (no user resize).

#### Scenario: Drag title moves panel

- **WHEN** the user drags the minimap title/handle to a new place over the Gantt main pane
- **THEN** the panel follows the pointer and the new coordinates are stored locally

#### Scenario: Close hides and disables preference

- **WHEN** the user clicks the minimap close control
- **THEN** the panel is hidden and the minimap toggle preference becomes off

#### Scenario: Restore position

- **WHEN** the user reopens Gantt with a previously saved minimap position and minimap enabled
- **THEN** the panel appears at that saved position

### Requirement: Minimap preference defaults on

The minimap enabled state MUST default to on for users without a saved preference, MUST be toggleable from the Gantt layers menu, and MUST be remembered locally with other Gantt layer preferences (or an adjacent local key). Turning the menu toggle off MUST hide the panel; turning it on MUST show the panel.

#### Scenario: First visit shows minimap

- **WHEN** a user opens Gantt with no saved minimap preference
- **THEN** the minimap is visible by default

#### Scenario: Toggle from layers menu

- **WHEN** the user unchecks minimap in the layers dropdown
- **THEN** the floating minimap is hidden until checked again
