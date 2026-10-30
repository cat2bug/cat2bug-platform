# defect-list-gantt-phase2 Specification

## Purpose

缺陷甘特二期：计划依赖与关联线、关键路径高亮、处理人资源负荷轨、计划基线对比、多选批量平移改期。

## Requirements

### Requirement: Plan dependencies between defects

The system SHALL allow authorized users to create, list, and delete plan dependency links between two defects in the same project. Phase-two link type MUST be finish-to-start (`FS`): the successor MUST NOT be planned to start before the predecessor’s plan end in validation messaging (hard block on create if it would form a cycle; schedule-violation on move MAY warn). Self-links and cross-project links MUST be rejected. Links MUST persist independently of the Gantt UI.

#### Scenario: Create FS link

- **WHEN** an editor creates an FS link from defect A to defect B in the same project and no cycle would result
- **THEN** the link is stored and subsequent Gantt loads for that project can retrieve it

#### Scenario: Reject cycle

- **WHEN** an editor attempts to add a link that would create a directed cycle
- **THEN** the operation fails and no link is stored

#### Scenario: Delete link

- **WHEN** an editor deletes an existing plan link
- **THEN** the link no longer appears in list responses or on the Gantt dependency layer

### Requirement: Gantt draws dependency lines

While the Gantt dependency (link-line) layer is enabled, the Gantt view MUST draw dashed visual connectors for FS links whose both endpoints are present in the current scheduled result set. Connectors MUST update when bars move within the view. Links with a missing endpoint in the current filter MUST NOT be drawn as dangling lines into empty space. Authorized editors MUST be able to create FS links by dragging a bar’s tail port to another bar’s head port (or head to tail), and MUST be able to select a connector and delete it with the Delete key.

#### Scenario: Both ends visible

- **WHEN** defects A and B are scheduled and visible and an FS link A→B exists and the dependency layer is on
- **THEN** a dashed connector is shown from A’s bar toward B’s bar

#### Scenario: Drag create FS

- **WHEN** an editor drags from defect A’s tail port to defect B’s head port (or A’s head to B’s tail)
- **THEN** an FS link is created with the correct predecessor → successor orientation

#### Scenario: Delete selected connector

- **WHEN** an editor selects a dependency connector and presses Delete
- **THEN** that plan link is removed

#### Scenario: Filtered-out endpoint

- **WHEN** an FS link exists but one endpoint is not in the current Gantt result set
- **THEN** no connector is drawn for that link on the axis

### Requirement: Critical path highlighting

The Gantt view SHALL offer a critical-path mode that, using only scheduled defects in the current Gantt result set, infers finish-to-start sequencing from plan dates (successor start day ≥ predecessor end day + 1) and highlights bars on a single representative longest-duration chain from early to late (non-critical bars MAY be dimmed). When multiple defects share the same schedule slot on that chain, only one representative bar SHALL be marked critical. Critical-path mode MUST NOT draw dependency-style connectors or arrows; the dependency layer remains the sole toggle for declared link connectors. Critical-path mode MUST NOT require user-declared FS links.

#### Scenario: Highlight start-to-finish chain

- **WHEN** the user enables critical-path mode and the current set has scheduled defects that can chain by plan dates
- **THEN** bars on one representative longest start-to-finish chain are visually distinguished without drawing path connectors

#### Scenario: Parallel same-slot defects

- **WHEN** several defects share the same plan window and would otherwise all lie on a longest path
- **THEN** only one representative among that parallel set is marked critical

#### Scenario: No chainable schedule

- **WHEN** critical-path mode is on but the current set has no scheduled defects (or none with plan dates)
- **THEN** no bars are marked critical

### Requirement: Handler resource load affordance

The Gantt view SHALL provide a resource-load affordance that aggregates planned occupancy by handler over the visible time window for defects in the current result set. Occupancy MUST be derived from plan start/end intervals. When a handler’s load exceeds the product-defined threshold for a day (or equivalent bucket), that bucket MUST be visually warned. When grouped by handler, the affordance SHOULD render as a per-handler load track under the group (normal thin segment / overload thicker segment). The affordance MUST NOT claim actual timesheet hours.

#### Scenario: Overload warning

- **WHEN** resource-load view is enabled and handler H has planned occupancy above the threshold on a visible day
- **THEN** that day/handler cell (or equivalent) shows an overload indication

#### Scenario: Empty window

- **WHEN** resource-load view is enabled and no scheduled defects fall in the visible window for any handler
- **THEN** the load affordance shows empty/zero state without errors

### Requirement: Plan baseline freeze and compare

Authorized users SHALL be able to update (freeze/overwrite) the current plan start/end of defects in the project into an active baseline snapshot. The Gantt view MUST be able to overlay baseline intervals against current plan bars so differences are visible. Updating again MUST replace the active baseline after confirmation (single-active-baseline). Read-only users MAY view baseline overlay if baseline data exists but MUST NOT update or clear it.

#### Scenario: Update baseline

- **WHEN** an editor confirms updating the baseline for the project
- **THEN** baseline intervals for included defects are stored as the active baseline

#### Scenario: Overlay differs from current plan

- **WHEN** baseline overlay is on and a defect’s current plan dates differ from its baseline dates
- **THEN** the Gantt shows both the current bar and a baseline representation so the shift is visible

### Requirement: Multi-select and batch shift

The Gantt view SHALL always allow box-selecting multiple scheduled bars on the axis blank area (no separate marquee-mode toggle). Selected rows MUST show full-row selected state. Authorized users MUST be able to batch-shift the selected defects’ plan intervals by dragging any selected bar horizontally by the same day delta while preserving each defect’s duration. The system MUST persist updates through the existing defect update authorization path and report partial failures if some updates fail. Clicking empty axis without a meaningful drag MUST clear the multi-selection. Modifier-click (Ctrl/⌘/Shift) MAY add or remove individual bars from the selection.

#### Scenario: Batch shift selection by drag

- **WHEN** the user box-selects three scheduled bars and drags one of them by +2 day columns
- **THEN** each selected defect’s plan start and end move forward by two days and durations stay the same

#### Scenario: Single bar when not multi-selected

- **WHEN** only one bar is selected (or none of a multi-set is under the pointer as a batch)
- **THEN** dragging that bar moves only that bar as in phase one
