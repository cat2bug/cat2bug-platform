## ADDED Requirements

### Requirement: Load track display-only; packing via arrange button or drag-yield

The handler resource-load affordance MUST continue to reflect planned occupancy for visualization and overload warning only. Enabling the load track MUST NOT by itself rewrite plan dates. Packing same-handler editable bars without avoidable gaps MUST be available through the toolbar auto-arrange action (`defect-gantt-level-resources`). Resolving overlaps created while dragging MUST be available through the layers drag-yield checkbox when enabled. Neither MUST be a silent side effect of load-track display, dependency drawing, or critical-path mode alone.

#### Scenario: Load track does not rewrite plans

- **WHEN** the user enables the resource-load layer without running auto-arrange or dragging under drag-yield
- **THEN** no defect plan start or plan end is changed solely because the load layer was enabled

#### Scenario: Full pack uses auto-arrange

- **WHEN** the user wants same-handler editable bars packed ASAP without gaps across the project candidate set
- **THEN** they use the toolbar auto-arrange action with preview and confirm

#### Scenario: Drag conflict uses drag-yield

- **WHEN** the user wants overlapping peers to yield while dragging a bar
- **THEN** they enable drag-yield in layers and drag the pinning bar
