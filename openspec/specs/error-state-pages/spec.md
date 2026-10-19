# error-state-pages Specification

## Purpose

404 / 401 / 团队禁用 / 项目禁用共用 ErrorStateShell 竖排错误页壳与主题插画。

## Requirements

### Requirement: Unified error page shell

The system SHALL render 404、401、团队禁用、项目禁用 pages with a shared vertical shell: mascot illustration, large status code (or word code), title, description, and action area. The shell MUST NOT use the list empty-state component `Cat2BugEmpty` or floating empty-box animation.

#### Scenario: 404 page layout

- **WHEN** the user opens a missing route that shows the 404 view
- **THEN** the page shows the 404 mascot, code `404`, title and description, and a primary action to go home

#### Scenario: 401 page layout

- **WHEN** the user opens the 401 unauthorized view
- **THEN** the page shows the 401 mascot, code `401`, title and description, a back action, and a home action

### Requirement: Theme-aware mascot assets

Error pages SHALL select light or dark mascot PNGs according to the application `themeMode` (`light` / `dark`). Disabled team and project pages SHALL share the same `disabled` illustration pair.

#### Scenario: Dark theme switches art

- **WHEN** `themeMode` is `dark`
- **THEN** each error page shows its corresponding `*-dark.png` illustration

#### Scenario: Light theme switches art

- **WHEN** `themeMode` is `light`
- **THEN** each error page shows its corresponding `*-light.png` illustration

### Requirement: Lock pages keep business actions

Team-lock and project-lock pages SHALL keep existing unlock/switch workflows (team popover / project list navigation) and SHALL still display lock remarks from user config when present.

#### Scenario: Team lock switch team

- **WHEN** the user is on the team-lock page and selects another unlocked team
- **THEN** the existing team-switch config update and navigation behavior still works

#### Scenario: Project lock select project

- **WHEN** the user is on the project-lock page and clicks select project
- **THEN** the app navigates to the project list as before
