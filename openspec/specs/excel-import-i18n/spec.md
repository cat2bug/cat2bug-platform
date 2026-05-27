## ADDED Requirements

### Requirement: Import column headers are matched across all supported locales

The system SHALL map Excel header row cells to `@Excel` fields using a candidate set that includes: the localized title for the request locale, the annotation `name()` default, the `i18nNameKey` translation in every locale listed in `LocaleUtils.MESSAGE_BUNDLE_LOCALES`, and any `alternateI18nKeys` translations for the same field. Column matching MUST NOT depend on the UI language matching the language used when the spreadsheet was created.

#### Scenario: English template imported with Chinese UI

- **WHEN** the user uploads a defect or case spreadsheet whose header row uses English column titles from an English template
- **AND** the UI and upload request use Chinese (`zh_CN`)
- **THEN** each importable column is bound to the correct entity field
- **AND** row data is read for required fields (e.g. defect name, state, type; case name, module, expect)

#### Scenario: Legacy case column keys are recognized

- **WHEN** the user uploads a case spreadsheet whose headers use translations from legacy keys `case.name_excel` or `case.expected_excel`
- **THEN** the columns map to `caseName` and `caseExpect` respectively

### Requirement: Defect state and type cell values resolve across locales

The system SHALL resolve defect state and type cells from display text in any supported locale to the correct `SysDefectStateEnum` and `SysDefectTypeEnum`. Resolution MUST use message bundle entries for all locales in `MESSAGE_BUNDLE_LOCALES`, enum constant names, and documented legacy aliases. Invalid values MUST be reported as import validation errors referencing the localized field label for the current request locale.

#### Scenario: Japanese state label with English UI

- **WHEN** a defect import row contains a state cell value equal to the Japanese message for `PROCESSING`
- **AND** the request locale is English
- **THEN** the row is stored with defect state `PROCESSING`

#### Scenario: Unknown state label fails validation

- **WHEN** a defect import row contains a state cell value that matches no known label in any supported locale
- **THEN** import validation fails for that row with an error referring to defect state

### Requirement: Defect priority cell values resolve across locales

The system SHALL resolve defect priority (dictionary type `defect_level`) from display text in any supported locale to the canonical dictionary value (`urgent`, `height`, `middle`, `low`). The resolver MUST index dictionary values, dictionary labels where present, and per-locale message translations for those values. Import MUST NOT treat the cell text as a message key lookup only.

#### Scenario: English priority label with Chinese UI

- **WHEN** a defect import row contains priority cell value equal to the English message for `urgent` (e.g. "Urgent")
- **AND** the request locale is Chinese
- **THEN** the stored defect level is `urgent`

### Requirement: Case import fields without localized enums remain language-agnostic

The system SHALL import case level values expressed as `P0`–`P4` or numeric level indicators without requiring locale-specific translation. Case text fields (name, expect, prerequisite, data, steps) MUST be accepted when headers match per the cross-locale header rule.

#### Scenario: Case level P2 imports regardless of UI language

- **WHEN** a case import row has level cell value `P2`
- **AND** the UI language is any supported locale
- **THEN** the case level is persisted correctly

### Requirement: Project-scoped reference data is matched literally

The system SHALL match deliverable (module path) and assignee (member nickname) cells by exact string against the current project's module paths and member nicknames. This behavior SHALL NOT vary by UI locale.

#### Scenario: Module path must exist in project

- **WHEN** a defect or case import row contains a module cell value that is not an existing module path in the project
- **THEN** import validation fails for that row with an error referring to the deliverable/module field

### Requirement: Import upload sends current UI language header

The system SHALL set the HTTP `language` header on defect and case Excel import uploads to match the active UI locale when the import dialog is opened and when the upload is submitted.

#### Scenario: Language header refreshed on submit

- **WHEN** the user changes UI language after opening the import dialog but before submitting the file
- **THEN** the submitted upload request includes the updated `language` header

### Requirement: Supported locales list stays in sync with message bundles

The system SHALL only treat locales listed in `LocaleUtils.MESSAGE_BUNDLE_LOCALES` as supported for Excel import header and label resolution. Adding a new `messages_*.properties` file MUST be accompanied by adding the corresponding `Locale` to that array per project i18n documentation.

#### Scenario: New language bundle is importable after maintenance

- **WHEN** a new language properties file is added and its locale is included in `MESSAGE_BUNDLE_LOCALES`
- **THEN** Excel templates with headers and enum labels in that language can be imported under any other UI language
