## ADDED Requirements

### Requirement: Minimap tracks axis width and scale changes

When the Gantt minimap is visible, after the time-axis column width or scale changes (including drag-driven threshold switches from `defect-gantt-axis-zoom`), the system MUST update the minimap bar marks and viewport rectangle so they remain proportional to the new axis content size and scroll position.

#### Scenario: After column resize minimap viewport still matches

- **WHEN** the user resizes axis column width while the minimap is visible
- **THEN** the red viewport rectangle and bar marks update to match the new axis content layout and current scroll region

#### Scenario: After scale threshold switch minimap updates

- **WHEN** a column-width drag switches scale (e.g. day to week) while the minimap is visible
- **THEN** the minimap overview and viewport rectangle reflect the new scale’s axis content
