## MODIFIED Requirements

### Requirement: Project business pages use namespaced module paths

The system SHALL expose project-scoped business UI under `/{teamKey}/{projectKey}/{module}/**`, where `{module}` is a singular segment from a documented whitelist aligned with existing project menus (including at least: `dashboard`, `defect`, `case`, `plan`, `document`, `module`, `notice`, `member`, `option`, `api`, `tools`, `code`). System, auth, and install routes (e.g. `/login`, `/setup`, `/system/**`) MUST remain outside this namespace. The segment `report` MUST NOT be a project module.

#### Scenario: Open defect list in project namespace

- **WHEN** a project member with access opens the defect list for a project that has path keys
- **THEN** the browser location is under `/{teamKey}/{projectKey}/defect` (or a documented child path under that module)

#### Scenario: System admin stays outside project prefix

- **WHEN** an administrator opens a platform system page such as dictionary management
- **THEN** the path remains under the existing system prefix and MUST NOT be nested under `/{teamKey}/{projectKey}`

#### Scenario: Report path is not a module

- **WHEN** a project member opens `/{teamKey}/{projectKey}/report`
- **THEN** the application does not render a report list or report detail
