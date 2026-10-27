## ADDED Requirements

### Requirement: Row editor uses shared widget catalog

In edit mode, the row layout editor's left-side widget catalog MUST be the shared workbench widget catalog defined by `workbench-widget-catalog` (domain groups, real previews with preserved aspect ratio, and advisory used-on-page marks). The editor MUST NOT present the previous flat title-only chip list as the primary catalog UI. Column pickers (empty-slot or add-on-stack selects) MAY remain compact text lists filtered by same-column `type` uniqueness. Placement, append/reorder, and same-column `type` uniqueness rules remain as specified by existing place-widget requirements.

#### Scenario: Edit mode shows grouped catalog

- **WHEN** a member enters edit mode on the personal workbench or an editable custom tab
- **THEN** the left catalog is grouped by product domain with preview frames
- **THEN** types already present on that surface show the used-on-page mark

#### Scenario: Same-column uniqueness unchanged

- **WHEN** a member tries to place a type that already exists in the target column
- **THEN** the editor still refuses the change or warns as before
- **THEN** the used-on-page mark alone does not block placement into a different column
