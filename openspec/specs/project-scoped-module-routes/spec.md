# project-scoped-module-routes Specification

## Requirements

### Requirement: Project business pages use namespaced module paths

The system SHALL expose project-scoped business UI under `/{teamKey}/{projectKey}/{module}/**`, where `{module}` is a singular segment from a documented whitelist aligned with existing project menus (including at least: `dashboard`, `defect`, `case`, `plan`, `report`, `document`, `module`, `notice`, `member`, `option`, `api`, `tools`, `code`). System, auth, and install routes (e.g. `/login`, `/setup`, `/system/**`) MUST remain outside this namespace.

#### Scenario: Open defect list in project namespace

- **WHEN** a project member with access opens the defect list for a project that has path keys
- **THEN** the browser location is under `/{teamKey}/{projectKey}/defect` (or a documented child path under that module)

#### Scenario: System admin stays outside project prefix

- **WHEN** an administrator opens a platform system page such as dictionary management
- **THEN** the path remains under the existing system prefix and MUST NOT be nested under `/{teamKey}/{projectKey}`

### Requirement: Empty or dashboard module opens project dashboard

Within the project namespace, the path `/{teamKey}/{projectKey}` and `/{teamKey}/{projectKey}/dashboard` SHALL open the project dashboard for an authorized member, after resolving path keys into the current project context.

#### Scenario: Dashboard via module segment

- **WHEN** a project member visits `/{teamKey}/{projectKey}/dashboard`
- **THEN** the project dashboard is shown and the current project context matches those keys

### Requirement: Switching project changes URL prefix

When the user switches the current project, the UI MUST navigate to the same logical module under the destination project’s `/{teamKey}/{projectKey}` prefix (or a documented default module if the current module does not apply). The system MUST NOT keep the previous project’s path keys in the address bar after a successful switch.

#### Scenario: Switch project from defect list

- **WHEN** a user viewing `/{teamA}/{projectA}/defect` switches to another accessible project `{teamB}/{projectB}`
- **THEN** the location becomes `/{teamB}/{projectB}/defect` (or the documented fallback) and data loads for the destination project

### Requirement: Legacy business paths redirect

The system SHALL redirect documented legacy project-business paths (including at least former `/defect`-style menu roots and `/project/git-repo/**` browse URLs) to the corresponding namespaced History paths when enough context (current project path keys and, for code routes, repository slug) is available. If context is insufficient, the system MUST land on a safe project-scoped entry or an explicit error rather than a broken page.

#### Scenario: Legacy git browse redirects when context known

- **WHEN** a user with a current project context opens a legacy `/project/git-repo/browse/{id}` URL that maps to a repository slug
- **THEN** the client navigates to `/{teamKey}/{projectKey}/code/{slug}` (or the matching deep link under that prefix)

### Requirement: Code module uses repository slug

Repository browse and related UI under the project namespace SHALL use `/{teamKey}/{projectKey}/code/{slug}/**` as the canonical browser path, where `{slug}` is the repository slug. Paths ending with `.git` under `code/` remain Git Smart HTTP and are out of scope for this SPA module requirement.

#### Scenario: Open code browse by slug

- **WHEN** a user with pull permission opens a repository from the project repository list
- **THEN** the browser location is `/{teamKey}/{projectKey}/code/{slug}` (or a child path under that prefix for tabs/deep links)
