## ADDED Requirements

### Requirement: Drag-yield push uses work calendar

While drag-yield (auto-level-on-drag) is enabled, peer push placements MUST use the project work calendar: peers shift later only onto working days, each peer’s **working-day** duration is preserved, and packing after the pin MUST NOT treat non-working days as fillable work capacity. The pinned bar’s own move/resize MUST also follow Gantt work-calendar schedule math.

#### Scenario: Push lands on next working day

- **WHEN** drag-yield is on and pushing peer B would otherwise start on a Saturday under a Mon–Fri calendar
- **THEN** B’s proposed start is the next working day after the pin/obstacles and its working-day duration is unchanged

## MODIFIED Requirements

### Requirement: Drag pin then push overlaps later

While auto-level-on-drag is enabled, when the user moves or resizes a single scheduled bar, the system SHALL treat the dragged bar’s preview plan range as pinned. Same first-handler editable peers that overlap the pinned range or earlier pushed peers MUST be shifted later only, preserving each peer’s **working-day** plan duration under the project work calendar, packing without avoidable **working-day** gaps after the pin. Multi-select batch horizontal shift MUST NOT run this push. On mouseup that persists the pinned bar’s date change, the system MUST also persist pushed peers’ new plan start/end.

#### Scenario: Overlap peer moves later on drag

- **WHEN** auto-level is on and the user drags bar A onto overlapping editable same-handler bar B
- **THEN** B’s preview moves to start on the next valid working day after A’s preview end with working-day duration unchanged, and on successful drop both A and B plan dates are updated accordingly

#### Scenario: Batch move skips push

- **WHEN** auto-level is on and the user batch-shifts multiple selected bars
- **THEN** no peer push proposals are applied from auto-level

#### Scenario: Toggle off no push

- **WHEN** auto-level is off and the user drags a bar onto an overlapping peer
- **THEN** peers are not automatically rescheduled by auto-level
