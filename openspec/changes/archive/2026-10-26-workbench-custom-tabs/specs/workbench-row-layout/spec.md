## ADDED Requirements

### Requirement: Former board widgets available in catalog

Statistic and chart widgets formerly limited to the board tab MUST be available in the workbench widget catalog for any editable surface (fixed work tab or editable custom tab). The system MUST NOT require a separate board tab to place those widgets.

#### Scenario: Place former board widget on work tab

- **WHEN** a member in edit mode on **工作台** drags a former board statistic widget onto an empty column and saves
- **THEN** that widget appears on the personal work layout
- **THEN** no board tab is required to view it

### Requirement: Extended presets including document outsider and custom

The system SHALL provide layout presets including at least `tester`, `developer`, `manager`, `document`, `outsider`, and `custom`. Preset `document` MUST seed a blank-friendly row layout that includes document-oriented widgets (not the full manager dashboard set). Preset `outsider` MUST define a default suitable for `project.outsider`. Preset `custom` MUST seed an empty hand-editable layout (blank slot row). First-time personal **工作台** seeding MUST map roles with priority create-by/admin → `manager`, develop → `developer`, tester → `tester`, otherwise `outsider`. Add-dialog create MUST offer `manager` | `tester` | `developer` | `document`. Edit-mode apply-preset MUST offer `manager` | `tester` | `developer` | `outsider` | `custom` and MUST show a confirmation dialog before replacing the current rows.

#### Scenario: First visit outsider

- **WHEN** a member whose project roles include only `project.outsider` opens the workbench with no saved personal layout
- **THEN** the personal **工作台** is seeded from the `outsider` preset

#### Scenario: Apply custom preset with confirm

- **WHEN** a member in edit mode chooses apply preset `custom` and confirms the dialog
- **THEN** the current surface rows are replaced with the custom blank layout
- **THEN** `preset` metadata reflects `custom` where applicable

#### Scenario: Cancel apply preset

- **WHEN** a member chooses apply preset but cancels the confirmation dialog
- **THEN** the current rows remain unchanged

## MODIFIED Requirements

### Requirement: Workbench shell with work and board tabs

The project workbench module SHALL present a fixed **工作台** tab that uses v3 row layout for the member's personal config, plus custom tabs defined by the `workbench-custom-tabs` capability. The module MUST NOT present a **看板** (`board`) tab. Personal work layout lives under the workbench config key historically named `tabs.work.rows` (or equivalent single personal rows root). Former board statistic widgets MUST be placeable via the widget catalog on editable surfaces rather than via a board checkbox list.

#### Scenario: Open project workbench

- **WHEN** a project member opens the project landing or dashboard module path
- **THEN** the fixed **工作台** tab control is presented
- **THEN** no **看板** tab control is presented
- **THEN** the work tab is the default selected surface when no valid local tab cache exists

#### Scenario: Board tab absent

- **WHEN** a member inspects the workbench tab bar after upgrade
- **THEN** there is no control named **看板**
- **THEN** former board widgets remain reachable from the catalog while editing

### Requirement: Version 3 row-column configuration

The system SHALL persist the personal **工作台** configuration keyed by `(userId, projectId)` with `version` equal to `3` and an ordered `rows` array (under `tabs.work` or an equivalent personal rows root). Custom tab layouts MUST also use `version: 3` row-column structures as specified by `workbench-custom-tabs`. Each row MUST contain an ordered `cols` array. Each column MUST store a relative `ratio` (positive number) and MAY store a widget instance `{ instanceId, type, props }` or be empty. The system MUST treat `instanceId` as the stable identity of a placed widget so that the same `type` MAY appear more than once. When loading legacy personal config that still contains `tabs.board`, the client MUST ignore board widgets for shell rendering and MUST NOT require board data to open the workbench.

#### Scenario: Save row layout

- **WHEN** a member saves an edited personal workbench with two rows and mixed column ratios
- **THEN** the stored personal config has `version: 3` and work rows reflecting that structure
- **THEN** each placed widget has a unique `instanceId` within the surface

#### Scenario: Unsupported older config

- **WHEN** the client loads a stored personal config with `version` less than `3` or without `rows`
- **THEN** the client applies the current role preset as a v3 row layout
- **THEN** the client does not attempt field-level migration of free-grid `layout` items

#### Scenario: Legacy board field ignored

- **WHEN** stored personal config still includes `tabs.board.widgets`
- **THEN** the client opens the workbench without a board tab
- **THEN** personal work rows still render

### Requirement: Role presets emit row layouts

Presets used for personal **工作台** seeding and edit-time apply (`tester`, `developer`, `manager`, `outsider`, `custom`, and create-time `document`) MUST define default `rows` for `version: 3`. Applying a preset from edit mode MUST replace the current surface rows only after confirmation. Presets MUST NOT require emitting `tabs.board.widgets`.

#### Scenario: Apply manager preset

- **WHEN** a member confirms applying the `manager` preset on the personal workbench
- **THEN** stored config `preset` is `manager` and `version` is `3`
- **THEN** work rows match the manager default row structure
- **THEN** no board widget list is required for the preset to be considered applied

### Requirement: Layout editor targets tab id

The row layout editor MUST accept a surface identifier and edit that surface's `rows` (personal work or a custom tab id). Saving MUST persist to the personal workbench API when the surface is **工作台**, and to the custom-tab API when the surface is a custom tab. The editor MUST reuse the same rows schema for all surfaces.

#### Scenario: Edit work tab

- **WHEN** a member enters edit mode on **工作台**
- **THEN** changes apply to the personal work rows
- **THEN** saving persists those rows via the personal workbench persistence API

#### Scenario: Edit custom tab

- **WHEN** a member with edit permission enters edit mode on a custom tab
- **THEN** changes apply to that tab's rows
- **THEN** saving persists those rows via the custom-tab persistence API
