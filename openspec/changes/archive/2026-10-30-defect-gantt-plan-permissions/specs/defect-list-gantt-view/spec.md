## MODIFIED Requirements

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
