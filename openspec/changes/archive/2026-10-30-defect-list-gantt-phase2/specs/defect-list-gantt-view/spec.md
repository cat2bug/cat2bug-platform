## ADDED Requirements

### Requirement: Gantt exposes phase-two scheduling tools

While the Gantt content view is active, the Gantt toolbar SHALL expose controls to toggle or enter: dependency layer, critical-path highlighting, resource-load affordance, baseline overlay, and marquee selection mode. These controls MUST NOT remove phase-one controls (group-by, scale, today). Defaults MUST keep phase-two layers off or in a non-blocking state so first-time users retain the phase-one interaction model until they enable a tool.

#### Scenario: Enable dependency layer from toolbar

- **WHEN** the user turns on the dependency layer from the Gantt toolbar
- **THEN** dependency connectors for in-view links become visible without leaving the Gantt view

#### Scenario: Phase-one tools remain

- **WHEN** the user opens Gantt with all phase-two layers off
- **THEN** group-by, scale, today, unscheduled pool, and single-bar drag behave as in phase one
