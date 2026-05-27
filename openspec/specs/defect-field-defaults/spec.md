# Spec: 缺陷字段默认值

## Requirements

### Requirement: Defect type priority and state defaults on persist

When a defect is created or updated through any supported channel (REST API, Excel inline create, Excel import, AddDefect, HandleDefect), the system SHALL apply defaults for missing type, priority, or state before persistence:

- defect type → `BUG` (`SysDefectTypeEnum.BUG`)
- defect priority (level) → `middle` (dictionary value `defect_level`)
- defect state → `PROCESSING` (`SysDefectStateEnum.PROCESSING`)

Blank strings for level MUST be treated as missing. Invalid enum or dictionary values MUST still fail validation; defaults MUST NOT mask invalid user input.

#### Scenario: API create with omitted type and level

- **WHEN** client POSTs a new defect with name and assignee but omits `defectType` and `defectLevel`
- **THEN** stored defect has type `BUG`, level `middle`, and state `PROCESSING`

#### Scenario: API update clears type

- **WHEN** client PUTs a defect with `defectType` explicitly null or cleared
- **THEN** stored defect type is `BUG`

#### Scenario: API update clears priority

- **WHEN** client PUTs a defect with `defectLevel` null or empty string
- **THEN** stored defect level is `middle`

### Requirement: Defect type and priority are optional on create

On create paths (form, Excel placeholder row, import row), the user MUST NOT be required to supply defect type, priority, or state. UI and import validation MUST NOT reject rows solely because those cells are empty, provided other required fields (e.g. name, assignee per existing rules) are present.

#### Scenario: Excel placeholder row without type

- **WHEN** user fills required Excel columns for a new row but leaves type and priority empty
- **THEN** row can be created successfully with type `BUG` and level `middle`

#### Scenario: Import row without state column value

- **WHEN** user imports a defect row with name and assignee but empty state and type cells
- **THEN** import succeeds and state is `PROCESSING` and type is `BUG`

### Requirement: Edit clears type or priority fall back to defaults

On edit paths (HandleDefect, Excel update of existing row, API update), when the user clears defect type or priority in the UI, the system SHALL persist the default values `BUG` and `middle` respectively.

#### Scenario: HandleDefect clears priority

- **WHEN** user clears the priority dropdown and saves in HandleDefect
- **THEN** defect level is stored as `middle`

#### Scenario: HandleDefect clears type

- **WHEN** user clears the type dropdown and saves in HandleDefect
- **THEN** defect type is stored as `BUG`

### Requirement: Defect state cannot be cleared in edit UI

On edit paths, the defect state control MUST NOT allow an empty selection. The state dropdown MUST NOT be clearable. Users MUST always have a valid state selected while editing an existing defect.

#### Scenario: HandleDefect state select not clearable

- **WHEN** user opens HandleDefect for an existing defect
- **THEN** the state selector does not offer a clear action and cannot be set to blank

#### Scenario: Excel edit existing row state not empty

- **WHEN** user edits the state cell on a row that already has a `defectId`
- **THEN** the system does not persist an empty state; the previous valid state is retained or the edit is rejected

### Requirement: Create UI may omit state with backend default

On AddDefect (and equivalent create dialogs), state MAY be omitted or hidden; on successful create the defect state MUST be `PROCESSING` without requiring user selection.

#### Scenario: AddDefect save without selecting state

- **WHEN** user saves a new defect from AddDefect without choosing state
- **THEN** created defect state is `PROCESSING`
