## ADDED Requirements

### Requirement: Non-member cannot enter project namespace as current context

Resolving `/{teamKey}/{projectKey}` (or any whitelisted module under that prefix) MUST require that the authenticated user is a member of the resolved project (or a documented system super-admin exemption). On failure the system MUST NOT set that project as `currentProjectId`, MUST NOT render the project business page as if authorized, and MUST send the user to a safe page (project list or an explicit error) instead of silently succeeding.

#### Scenario: Team member opens another project's namespaced URL

- **WHEN** a team member who is not a member of project A opens `/{teamKey}/{projectKeyOfA}/defect` (or `/dashboard`)
- **THEN** the application does not establish project A as the current project context
- **THEN** the defect or workbench page for project A is not shown as authorized content
- **THEN** the user is redirected to the project list or an explicit access error

#### Scenario: Member opens own project namespace

- **WHEN** a project member opens `/{teamKey}/{projectKey}/defect` for a project they belong to
- **THEN** the current project context matches those keys and the module renders as today
