## ADDED Requirements

### Requirement: Empty defect type state and priority cells use defaults on import

During defect Excel import, when the type, state, or priority cell is empty or whitespace-only, the system MUST NOT treat the field as a validation error for emptiness. After optional label resolution for non-empty cells, the system MUST apply defect defaults (`BUG`, `PROCESSING`, `middle`) per `defect-field-defaults` before insert.

#### Scenario: Import empty type and state with Chinese UI

- **WHEN** a defect import row has empty type and state cells and valid name and assignee
- **AND** the request locale is Chinese
- **THEN** the row is imported with type `BUG` and state `PROCESSING`

#### Scenario: Import empty priority

- **WHEN** a defect import row has empty priority cell and valid required fields
- **THEN** the row is imported with level `middle`

## MODIFIED Requirements

### Requirement: Import column headers are matched across all supported locales

The system SHALL map Excel header row cells to `@Excel` fields using a candidate set that includes: the localized title for the request locale, the annotation `name()` default, the `i18nNameKey` translation in every locale listed in `LocaleUtils.MESSAGE_BUNDLE_LOCALES`, and any `alternateI18nKeys` translations for the same field. Column matching MUST NOT depend on the UI language matching the language used when the spreadsheet was created.

#### Scenario: English template imported with Chinese UI

- **WHEN** the user uploads a defect or case spreadsheet whose header row uses English column titles from an English template
- **AND** the UI and upload request use Chinese (`zh_CN`)
- **THEN** each importable column is bound to the correct entity field
- **AND** row data is read for required fields (e.g. defect name and assignee; case name, module, expect)

#### Scenario: Legacy case column keys are recognized

- **WHEN** the user uploads a case spreadsheet whose headers use translations from legacy keys `case.name_excel` or `case.expected_excel`
- **THEN** the columns map to `caseName` and `caseExpect` respectively

### Requirement: Defect state and type cell values resolve across locales

The system SHALL resolve defect state and type cells from display text in any supported locale to the correct `SysDefectStateEnum` and `SysDefectTypeEnum`. Resolution MUST use message bundle entries for all locales in `MESSAGE_BUNDLE_LOCALES`, enum constant names, and documented legacy aliases. Invalid non-empty values MUST be reported as import validation errors referencing the localized field label for the current request locale. Empty cells MUST NOT trigger invalid-value errors and MUST receive defaults on import.

#### Scenario: Japanese state label with English UI

- **WHEN** a defect import row contains a state cell value equal to the Japanese message for `PROCESSING`
- **AND** the request locale is English
- **THEN** the row is stored with defect state `PROCESSING`

#### Scenario: Unknown state label fails validation

- **WHEN** a defect import row contains a non-empty state cell value that matches no known label in any supported locale
- **THEN** import validation fails for that row with an error referring to defect state
