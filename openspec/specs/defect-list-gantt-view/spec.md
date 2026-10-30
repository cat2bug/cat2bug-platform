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

Users with `system:defect:gantt:plan` MAY attempt Gantt plan/field updates via `PUT /system/defect/plan-node` (not via general `system:defect:edit`). The system MUST only allow mutating a defect’s plan interval or Gantt-driven `handleBy` / `moduleId` when the user is a **project plan admin** (`project.create-by` or `project.admin`, including project `createBy`) **or** the user is among that defect’s handlers (`handleBy` / `handleByList`). Project plan admins MUST be able to mutate any defect’s node under the operations below; other holders of the Gantt plan permission MUST only mutate defects they handle. Granting Gantt plan permission MUST NOT imply `system:defect:edit`.

When authorized for that defect, the user MUST be able to: (1) drag an unscheduled defect onto the time axis to set plan start/end with default duration of **one working day** at the (aligned) drop date under the project work calendar; (2) drag a bar horizontally to shift the interval without changing **working-day** duration; (3) drag bar edges to change start or end with a minimum duration of **one working day** on the day scale; (4) drop onto a handler group row to set `handleBy` to only that handler and update plan times as implied by the drop; (5) drop onto a deliverable group row to set `moduleId` to that module without changing handlers; (6) drop onto empty axis space to change only plan times. Updates MUST persist through the dedicated plan-node path; the server MUST reject unauthorized plan/node mutations. Double-click or equivalent on a bar/row MUST open the existing defect detail surface. Schedule math MUST respect the project work calendar as specified in the Gantt work-calendar requirements. Bars the user cannot edit MUST NOT be draggable for plan changes.

#### Scenario: Unscheduled drop creates one-day plan

- **WHEN** an authorized handler (or project plan admin) drags an unscheduled defect onto a working calendar day on the axis
- **THEN** the defect gains plan start on that day (or the aligned next working day if required) and plan end such that the working-day duration is one, and moves to the scheduled region

#### Scenario: Drop on handler row replaces handlers

- **WHEN** grouping is by handler and an authorized user drops a defect onto handler A’s group row
- **THEN** the defect’s handlers become only A and its plan times update according to the drop position under the work calendar

#### Scenario: Drop on deliverable row changes module only

- **WHEN** grouping is by deliverable and an authorized user drops a defect onto module M’s group row
- **THEN** the defect’s module becomes M, handlers are unchanged, and plan times update according to the drop position under the work calendar

#### Scenario: Empty axis drop keeps handlers and module

- **WHEN** an authorized user drops a bar onto empty time-axis space (not on a group row)
- **THEN** only plan start/end change and handlers and module stay the same

#### Scenario: Non-handler cannot drag others’ bars

- **WHEN** a non-admin user with Gantt plan permission is not among defect B’s handlers
- **THEN** they MUST NOT successfully change B’s plan dates or Gantt-driven handlers/module via the Gantt

#### Scenario: Project admin can drag any bar

- **WHEN** a project create-by or project admin with Gantt plan permission drags another member’s scheduled bar
- **THEN** the plan update is accepted

#### Scenario: Develop can drag own bars without defect edit

- **WHEN** a `project.develop` user has `system:defect:gantt:plan` but not `system:defect:edit`, and is among defect A’s handlers
- **THEN** they MAY update A’s plan via the Gantt plan-node API and MUST NOT gain general defect edit capability

### Requirement: Left columns stay minimal

The Gantt left list SHALL show group headers and defect identity (project number and name) plus the duplicate indicator when applicable. It MUST NOT add a dedicated plan start/end text column in phase one.

#### Scenario: No start-end text column

- **WHEN** the user views the Gantt left list
- **THEN** there is no separate column whose sole purpose is displaying plan start/end timestamps

### Requirement: Gantt exposes phase-two scheduling tools

While the Gantt content view is active, the Gantt toolbar SHALL expose a layers control (icon dropdown) to toggle: link lines (dependencies), critical-path highlighting, resource-load affordance, baseline overlay, and **minimap**, plus an action to update the active baseline. The update-baseline action MUST be available only to project plan admins (`project.create-by` / `project.admin`); other users MUST NOT freeze or clear the baseline from this UI. The minimap item SHOULD be visually separated from content overlay toggles (e.g. divider). Group-by (handler / deliverable) MUST remain available as tabs on the left header. Scale and today controls MUST remain on the toolbar. Defaults MUST keep content overlay layers (link lines, critical path, load, baseline) off or non-blocking until enabled; **minimap defaults to on**. Layer on/off (including minimap) MUST be remembered locally. Multi-select box selection MUST remain available without a separate mode toggle (see `defect-list-gantt-phase2`). Minimap behavior is specified in `defect-gantt-minimap`.

#### Scenario: Enable link lines from layers menu

- **WHEN** the user turns on link lines from the Gantt layers menu
- **THEN** dependency connectors for in-view links become visible without leaving the Gantt view

#### Scenario: Toggle minimap from layers menu

- **WHEN** the user turns off minimap from the Gantt layers menu
- **THEN** the floating time-axis minimap is hidden without leaving the Gantt view

#### Scenario: Phase-one tools remain

- **WHEN** the user opens Gantt with all content overlay layers off
- **THEN** group-by, scale, today, unscheduled pool, and single-bar drag behave as in phase one (minimap MAY still show when its preference is on)

#### Scenario: Non-admin cannot freeze baseline from layers

- **WHEN** a non-admin member opens the layers menu
- **THEN** the update-baseline action is unavailable or disabled

### Requirement: Auto-arrange action in Gantt layers menu

While the Gantt content view is active, the system SHALL provide an auto-arrange action in the Gantt layers dropdown (below freeze-baseline) that starts the one-shot level-resources preview flow. The action MUST show an icon and MUST be disabled when the user lacks defect edit permission. Activating it MUST NOT change table/Excel switching, unscheduled pool behavior, or unrelated local preference keys.

#### Scenario: Layers action visible in Gantt

- **WHEN** the user opens the Gantt layers menu with edit permission
- **THEN** an auto-arrange action is available below freeze-baseline

#### Scenario: Control disabled without edit permission

- **WHEN** the user is on the Gantt view without defect edit permission
- **THEN** the auto-arrange action is unavailable or disabled

### Requirement: Drag-yield checkbox naming in layers

The layers menu checkbox that enables push-on-drag MUST be labeled as drag-yield (or equivalent localized copy such as「拖动避让」), not as the primary「自动排列」name reserved for the toolbar one-shot action. Its persistence key MAY remain `autoLevel` under `defect.gantt.layers`.

#### Scenario: Layers label is drag-yield

- **WHEN** the user opens the Gantt layers menu
- **THEN** the push-on-drag checkbox uses drag-yield oriented wording rather than the toolbar auto-arrange label alone

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

