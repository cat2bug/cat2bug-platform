## ADDED Requirements

### Requirement: Auto-level push candidate set

When auto-level-on-drag is enabled, the system SHALL only push defects that are (1) not closed, (2) scheduled with both plan start and plan end, and (3) editable by the current user under auto-level rules. Closed and unscheduled defects MUST NOT be pushed. Defects the user cannot edit MUST NOT be moved by the push.

#### Scenario: Closed excluded

- **WHEN** a same-handler bar is closed and overlaps the dragged landing range
- **THEN** it is not pushed and does not block pushing other editable peers

#### Scenario: Unscheduled excluded

- **WHEN** a defect lacks plan start or plan end
- **THEN** it is not pushed

### Requirement: Auto-level edit permission

The system SHALL allow users with project role `project.create-by` or `project.admin` (and defect edit permission) to push all editable non-closed scheduled peers in the handler lane. Other users with defect edit permission SHALL only push peers where the current user is among `handleBy`. Users without defect edit permission MUST NOT enable auto-level.

#### Scenario: Project admin may push all editable peers

- **WHEN** a project admin with defect edit permission drags with auto-level on
- **THEN** overlapping non-closed scheduled peers they can edit MAY be proposed for later plan dates

#### Scenario: Member pushes only own handled

- **WHEN** a non-admin member with defect edit permission drags with auto-level on
- **THEN** only overlapping peers whose handlers include that member MAY be pushed

#### Scenario: No edit permission

- **WHEN** the user lacks `system:defect:edit`
- **THEN** the auto-level checkbox is unavailable or disabled

### Requirement: Air obstacles

When computing push placements, the system MUST NOT treat non-editable or closed bars as resource occupancy that blocks placement. Overlap with such bars MAY remain. Among the pinned bar and editable peers being packed in the lane, the system MUST NOT leave overlapping plan intervals after push.

#### Scenario: Ignore non-editable overlap

- **WHEN** a peer would still overlap a non-editable scheduled defect after being pushed past the pinned bar
- **THEN** the peer is still placed immediately after the pinned/pushed obstacles and is not delayed solely to avoid that non-editable bar

### Requirement: Drag pin then push overlaps later

While auto-level-on-drag is enabled, when the user moves or resizes a single scheduled bar, the system SHALL treat the dragged bar’s preview plan range as pinned. Same first-handler editable peers that overlap the pinned range or earlier pushed peers MUST be shifted later only, preserving each peer’s plan duration, packing without avoidable gaps after the pin. Multi-select batch horizontal shift MUST NOT run this push. On mouseup that persists the pinned bar’s date change, the system MUST also persist pushed peers’ new plan start/end.

#### Scenario: Overlap peer moves later on drag

- **WHEN** auto-level is on and the user drags bar A onto overlapping editable same-handler bar B
- **THEN** B’s preview moves to start after A’s preview end with duration unchanged, and on successful drop both A and B plan dates are updated accordingly

#### Scenario: Batch move skips push

- **WHEN** auto-level is on and the user batch-shifts multiple selected bars
- **THEN** no peer push proposals are applied from auto-level

#### Scenario: Toggle off no push

- **WHEN** auto-level is off and the user drags a bar onto an overlapping peer
- **THEN** peers are not automatically rescheduled by auto-level
