## ADDED Requirements

### Requirement: Load track occupancy ignores non-working days

When the resource-load affordance is enabled, planned occupancy used for load visualization and overload warning MUST count only working days under the project work calendar. Non-working days (work-week off-days and `holiday` exceptions) MUST NOT contribute occupancy toward overload. Makeup `workday` exceptions MUST count as working capacity days.

#### Scenario: Weekend not counted as load

- **WHEN** a bar’s natural span includes Saturday–Sunday under a Mon–Fri calendar and the load track is on
- **THEN** those weekend columns do not add handler occupancy for overload calculation

#### Scenario: Makeup day counts as load capacity day

- **WHEN** a bar covers a Sunday marked as makeup `workday`
- **THEN** that day contributes to load occupancy like other working days

## MODIFIED Requirements

### Requirement: Load track display-only; packing via arrange button or drag-yield

The handler resource-load affordance MUST continue to reflect planned occupancy for visualization and overload warning only, with occupancy computed on working days as specified for the work calendar. Enabling the load track MUST NOT by itself rewrite plan dates. Packing same-handler editable bars without avoidable gaps MUST be available through the toolbar auto-arrange action (`defect-gantt-level-resources`) using work-calendar schedule math. Resolving overlaps created while dragging MUST be available through the layers drag-yield checkbox when enabled, also under the work calendar. Neither MUST be a silent side effect of load-track display, dependency drawing, or critical-path mode alone.

#### Scenario: Load track does not rewrite plans

- **WHEN** the user enables the resource-load layer without running auto-arrange or dragging under drag-yield
- **THEN** no defect plan start or plan end is changed solely because the load layer was enabled

#### Scenario: Full pack uses auto-arrange

- **WHEN** the user wants same-handler editable bars packed ASAP without gaps across the project candidate set
- **THEN** they use the toolbar auto-arrange action with preview and confirm

#### Scenario: Drag conflict uses drag-yield

- **WHEN** the user wants overlapping peers to yield while dragging a bar
- **THEN** they enable drag-yield in layers and drag the pinning bar
