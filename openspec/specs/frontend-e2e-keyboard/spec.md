# frontend-e2e-keyboard Specification

## Purpose
TBD - created by archiving change vue3-vite-pinia-migration. Update Purpose after archive.
## Requirements
### Requirement: Playwright keyboard E2E baseline on Vue 2

Before framework migration begins, a Playwright E2E keyboard regression suite SHALL be established on the current Vue 2 codebase as a behavior baseline.

#### Scenario: E2E test directory populated

- **WHEN** Phase 0 completes
- **THEN** `cat2bug-platform-ui/e2e/` MUST contain at least one spec file per critical keyboard domain listed below

#### Scenario: Backend dependency documented

- **WHEN** E2E tests run via `npm run test:e2e`
- **THEN** tests MUST require backend on port 2020 (matching current `playwright.config.cjs` webServer setup) and MUST use demo/admin test credentials

### Requirement: Login and navigation keyboard E2E

The Playwright keyboard E2E suite SHALL cover login form submission and post-login sidebar navigation without mouse interaction.

#### Scenario: Login form keyboard flow

- **WHEN** the user tabs through the login form and presses Enter on the submit button
- **THEN** authentication MUST succeed with valid credentials without requiring mouse interaction

#### Scenario: Sidebar keyboard navigation

- **WHEN** the user navigates the sidebar menu via keyboard after login
- **THEN** focus MUST move predictably and Enter MUST activate the focused menu item

### Requirement: Defect module keyboard E2E

The Playwright keyboard E2E suite SHALL cover defect list selection, detail open/close, and create-form tab order on the defect module.

#### Scenario: Defect list keyboard selection

- **WHEN** the user opens the defect list and presses Up/Down arrows
- **THEN** row selection MUST change visually and the focused row MUST be distinguishable

#### Scenario: Defect open and close via keyboard

- **WHEN** the user selects a defect row and presses Enter
- **THEN** the defect detail drawer/dialog MUST open; pressing Escape MUST close it

#### Scenario: Defect form Tab order

- **WHEN** the user opens the defect create form and tabs through fields
- **THEN** focus order MUST follow a logical sequence without traps or skipped interactive elements

### Requirement: DatePicker and CommandPalette E2E

The Playwright keyboard E2E suite SHALL cover DatePicker panel open/close and CommandPalette global shortcut activation.

#### Scenario: DatePicker keyboard open/close

- **WHEN** a date picker field is focused and the user presses Down arrow
- **THEN** the calendar panel MUST open; pressing Escape MUST close it without submitting

#### Scenario: CommandPalette trigger

- **WHEN** the user presses the configured global shortcut (e.g. Ctrl+K or platform equivalent)
- **THEN** the CommandPalette MUST appear and accept keyboard input

### Requirement: E2E gate for migration phases

Each migration phase MUST pass the full keyboard E2E suite before proceeding to the next phase.

#### Scenario: Phase completion gate

- **WHEN** a migration phase is marked complete
- **THEN** all Playwright keyboard E2E tests MUST pass on Chromium; WebKit failures MUST be triaged and tracked

#### Scenario: No regression tolerance for keyboard

- **WHEN** an E2E keyboard test fails after a phase migration
- **THEN** the phase MUST NOT be signed off until the failure is fixed or the test is updated with documented intentional behavior change approved in design.md

