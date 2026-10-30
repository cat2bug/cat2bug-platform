## MODIFIED Requirements

### Requirement: Gantt exposes phase-two scheduling tools

While the Gantt content view is active, the Gantt toolbar SHALL expose a layers control (icon dropdown) to toggle: link lines (dependencies), critical-path highlighting, resource-load affordance, baseline overlay, and **minimap**, plus an action to update the active baseline. The minimap item SHOULD be visually separated from content overlay toggles (e.g. divider). Group-by (handler / deliverable) MUST remain available as tabs on the left header. Scale and today controls MUST remain on the toolbar. Defaults MUST keep content overlay layers (link lines, critical path, load, baseline) off or non-blocking until enabled; **minimap defaults to on**. Layer on/off (including minimap) MUST be remembered locally. Multi-select box selection MUST remain available without a separate mode toggle (see `defect-list-gantt-phase2`). Minimap behavior is specified in `defect-gantt-minimap`.

#### Scenario: Enable link lines from layers menu

- **WHEN** the user turns on link lines from the Gantt layers menu
- **THEN** dependency connectors for in-view links become visible without leaving the Gantt view

#### Scenario: Toggle minimap from layers menu

- **WHEN** the user turns off minimap from the Gantt layers menu
- **THEN** the floating time-axis minimap is hidden without leaving the Gantt view

#### Scenario: Phase-one tools remain

- **WHEN** the user opens Gantt with all content overlay layers off
- **THEN** group-by, scale, today, unscheduled pool, and single-bar drag behave as in phase one (minimap MAY still show when its preference is on)
