# defect-list-gantt-view Specification

## Purpose

缺陷列表第三内容视图「甘特」：已排期轴 + 未排期池；处理人/交付物分组；拖拽改期与字段写回。

## Requirements

### Requirement: Defect list offers a Gantt content view

The defect page SHALL provide a third content view mode alongside table and Excel, labeled for Gantt, using the same view-switch control group. The system MUST remember the last selected content view (including Gantt) in local storage for the project session consistent with existing table/Excel preference behavior. While Gantt is active, the system MUST apply the same defect tab and toolbar filters as the table view when loading defects.

#### Scenario: Switch to Gantt from table

- **WHEN** the user selects the Gantt option in the defect content view switch
- **THEN** the main content area shows the Gantt view for the current tab/filter query instead of the table or Excel view

#### Scenario: View preference restored

- **WHEN** the user previously selected Gantt and later reopens the defect page with the same preference key available
- **THEN** the Gantt view is selected again without requiring the user to re-click the switch

### Requirement: Gantt layout with scheduled and unscheduled regions

The Gantt view SHALL show an upper region for defects that have both plan start and plan end (or a coherent plan interval as implemented) and a lower unscheduled pool for defects lacking plan times. The upper region MUST use a left row list and a right time axis that scroll vertically in sync. The time axis MUST support day, week, and month scales, show a today marker, and shade weekend days.

#### Scenario: Unscheduled defects appear in the pool

- **WHEN** a defect in the current filter has no plan start/end suitable for a bar
- **THEN** it appears in the lower unscheduled pool and not as a bar in the upper time axis

#### Scenario: Change time scale

- **WHEN** the user selects week scale
- **THEN** the time axis headers and bar positions update to the week scale while keeping the same defects and grouping

### Requirement: Grouping by handler or deliverable

The Gantt view SHALL let the user switch grouping between handler and deliverable. Default grouping MUST be by handler. The system MUST persist the last grouping choice locally with the view preferences. When grouped by handler, a defect with multiple handlers MUST appear once under each handler row, and each mirrored row MUST show a duplicate indicator whose affordance lists the other handlers. When grouped by deliverable, each defect MUST appear once under its module group without handler mirroring. Handler is required on defects; the Gantt view MUST NOT provide an “unassigned handler” group.

#### Scenario: Multi-handler mirroring

- **WHEN** grouping is by handler and a defect has two handlers A and B
- **THEN** the defect bar/row appears under both A and B with a duplicate indicator on each

#### Scenario: Switch to deliverable grouping

- **WHEN** the user switches grouping to deliverable
- **THEN** defects are listed under module groups without multi-handler mirror rows

### Requirement: Bar color follows defect state

Gantt bars MUST use the same defect-state color tokens as elsewhere in the product (`--defect-state-*`), so processing, audit, resolved, rejected, and closed states remain visually consistent with table/detail.

#### Scenario: Processing bar color

- **WHEN** a scheduled defect is in the processing state
- **THEN** its Gantt bar uses the processing state color token

### Requirement: Drag schedule and field updates

Authorized users MUST be able to: (1) drag an unscheduled defect onto the time axis to set plan start/end with default duration of one day at the drop date; (2) drag a bar horizontally to shift the interval without changing duration; (3) drag bar edges to change start or end with a minimum duration of one day on the day scale; (4) drop onto a handler group row to set `handleBy` to only that handler and update plan times as implied by the drop; (5) drop onto a deliverable group row to set `moduleId` to that module without changing handlers; (6) drop onto empty axis space to change only plan times. Updates MUST persist through the existing defect update path and respect edit permissions. Double-click or equivalent on a bar/row MUST open the existing defect detail surface.

#### Scenario: Unscheduled drop creates one-day plan

- **WHEN** the user drags an unscheduled defect onto a calendar day on the axis
- **THEN** the defect gains plan start on that day and plan end one day later (per product day semantics) and moves to the scheduled region

#### Scenario: Drop on handler row replaces handlers

- **WHEN** grouping is by handler and the user drops a defect onto handler A’s group row
- **THEN** the defect’s handlers become only A and its plan times update according to the drop position

#### Scenario: Drop on deliverable row changes module only

- **WHEN** grouping is by deliverable and the user drops a defect onto module M’s group row
- **THEN** the defect’s module becomes M, handlers are unchanged, and plan times update according to the drop position

#### Scenario: Empty axis drop keeps handlers and module

- **WHEN** the user drops a bar onto empty time-axis space (not on a group row)
- **THEN** only plan start/end change and handlers and module stay the same

### Requirement: Left columns stay minimal

The Gantt left list SHALL show group headers and defect identity (project number and name) plus the duplicate indicator when applicable. It MUST NOT add a dedicated plan start/end text column in phase one.

#### Scenario: No start-end text column

- **WHEN** the user views the Gantt left list
- **THEN** there is no separate column whose sole purpose is displaying plan start/end timestamps

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

