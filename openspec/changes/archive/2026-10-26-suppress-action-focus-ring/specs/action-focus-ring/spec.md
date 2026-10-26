## ADDED Requirements

### Requirement: Action controls leave no wild focus outline after pointer activation

After the user activates an action control with a pointer (mouse or trackpad click), the UI MUST NOT show a residual browser or Element Plus default focus outline (blue `outline` / equivalent focus ring) on that control. Action controls include primary UI actions such as `el-button`, tab items, navbar icon actions, and sidebar menu / toggle controls.

#### Scenario: Click toolbar button

- **WHEN** the user clicks a toolbar or form-footer `el-button` with the pointer
- **THEN** no blue focus outline MUST remain visible on that button after the click

#### Scenario: Click tab item

- **WHEN** the user clicks an `el-tabs` tab item with the pointer
- **THEN** no wild focus outline MUST remain on the tab item (active underline / selected styles MAY remain)

#### Scenario: Click navbar or sidebar action

- **WHEN** the user clicks a top-right navbar icon action or a sidebar menu / collapse control with the pointer
- **THEN** no wild focus outline MUST remain on that control

### Requirement: Form field focus rings remain controlled

Form fields that already use the Cat2Bug field focus tokens MUST continue to show their controlled focus ring when focused. This change MUST NOT remove or weaken `--cat2bug-field-focus-*` / unified focus-ring styling for inputs, textareas, selects, switches, or upload focus shells.

#### Scenario: Focus text input

- **WHEN** the user focuses an `el-input` text field by click or keyboard
- **THEN** the controlled field focus ring MUST still appear as before this change

### Requirement: Keyboard navigation highlights remain class-driven

Keyboard navigation indicators that rely on application CSS classes (for example `list-query-toolbar-nav-focused`, `is-keyboard-focus`, `defect-tab-nav-focused`) MUST continue to render when those classes are applied. Suppressing wild outlines MUST NOT disable or override those class-based highlights.

#### Scenario: List query keyboard nav highlight

- **WHEN** list-query keyboard navigation marks a control with `list-query-toolbar-nav-focused` (or equivalent nav-focused class)
- **THEN** the existing keyboard highlight styles MUST still be visible
