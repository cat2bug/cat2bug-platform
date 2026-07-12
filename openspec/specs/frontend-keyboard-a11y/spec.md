# frontend-keyboard-a11y Specification

## Purpose
TBD - created by archiving change vue3-vite-pinia-migration. Update Purpose after archive.
## Requirements
### Requirement: Keyboard Adapter Layer

The frontend SHALL introduce a Keyboard Adapter Layer that isolates all Element Plus DOM selectors and internal component API access from business keyboard logic.

#### Scenario: Adapter directory structure

- **WHEN** the migration architecture is in place
- **THEN** Element-specific DOM/API access MUST live under `src/adapters/element-plus/` and MUST NOT be scattered in view components

#### Scenario: Adapter swap on UI library change

- **WHEN** Element Plus internal APIs or CSS class names change in a future upgrade
- **THEN** only adapter files MUST require modification; composables and business logic MUST remain unchanged

### Requirement: Mixin to composable migration

All 40 keyboard/a11y mixins and 23 Element-coupled plugins SHALL be refactored to Vue 3 composables while preserving behavior.

#### Scenario: DatePicker keyboard behavior

- **WHEN** an `el-date-picker` receives focus
- **THEN** the panel MUST NOT auto-open; Down/Up/Enter/Space MUST open it; Escape MUST close it (matching current `date-picker-kbd` plugin behavior)

#### Scenario: Dropdown keyboard navigation

- **WHEN** a dropdown is focused via keyboard
- **THEN** arrow key navigation and selection MUST match current `dropdown-kbd` plugin behavior on Element Plus

#### Scenario: List query keyboard navigation

- **WHEN** the user navigates a data list (defect list, project list, etc.) via keyboard
- **THEN** Up/Down selection, Enter to open, Escape to close MUST match current `list-query-keyboard-nav` mixin behavior

#### Scenario: Command palette

- **WHEN** the user triggers the global command palette shortcut
- **THEN** the CommandPalette MUST open, accept keyboard navigation, and execute actions identically to the Vue 2 version

#### Scenario: Form field hints

- **WHEN** the user tabs through form fields
- **THEN** contextual keyboard hints MUST display per current `form-field-hints` and `page-action-hints` mixin behavior

#### Scenario: Shortcut plugin API

- **WHEN** a page registers shortcuts via `$shortcut.registerPage`
- **THEN** the Pinia/Vue 3 equivalent API MUST support the same scope key, action registration, and reserved key handling

### Requirement: Unit test contract preservation

Existing keyboard unit tests SHALL continue to pass against refactored implementations.

#### Scenario: Date picker unit tests

- **WHEN** `tests/unit/utils/date-picker-kbd.spec.js` and related specs run after migration
- **THEN** all tests MUST pass without weakening assertions

#### Scenario: Shortcut unit tests

- **WHEN** `tests/unit/plugins/shortcut/*.spec.js` run after migration
- **THEN** reserved key blocking and UI layer detection MUST pass all existing test cases

#### Scenario: New adapter unit tests

- **WHEN** an Element Plus adapter is created for a keyboard domain (date-picker, dropdown, table, select)
- **THEN** it MUST have at least one dedicated unit test file covering query/focus/visibility helpers

### Requirement: Dialog and drawer keyboard close

Dialog, drawer, and overlay close behaviors via keyboard MUST be preserved across all modules.

#### Scenario: Escape closes topmost overlay

- **WHEN** a dialog or drawer is open and the user presses Escape
- **THEN** the topmost overlay MUST close without affecting underlying page state incorrectly

#### Scenario: Defect form drawer shortcuts

- **WHEN** the defect create/edit drawer is open
- **THEN** all defect drawer keyboard shortcuts (`defect-drawer-shortcuts` plugin) MUST function on Element Plus drawer components

