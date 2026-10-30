## ADDED Requirements

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
