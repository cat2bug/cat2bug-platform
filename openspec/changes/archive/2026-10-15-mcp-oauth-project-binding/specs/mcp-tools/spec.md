## REMOVED Requirements

### Requirement: Project can be located by key, ID, or Git remote

**Reason**: The project is fixed by the OAuth authorization; tools no longer take a `project` argument and cannot reach other projects.
**Migration**: Use `get_current_project` to see the bound project; to work on another project, log in and authorize again in the client.

## ADDED Requirements

### Requirement: Current project is reported by the bound token

The system SHALL provide `get_current_project`, returning the project bound to the access token (ID, team and project keys, names), the modules usable by the user in that project, and the project's code repositories. `resolve_project_by_git_remote` SHALL look up a repository only inside the bound project and MUST fail with a clear message when the URL belongs to another project or is not hosted on the platform. No other tool MUST accept a `project` argument.

#### Scenario: Read current project

- **WHEN** a client authorized for project "project-58" calls `get_current_project`
- **THEN** the response contains project "project-58", its usable modules, and its repositories

#### Scenario: Remote of another project

- **WHEN** a client authorized for project "project-58" calls `resolve_project_by_git_remote` with the clone URL of a repository in another project
- **THEN** the call fails and says the repository is not in the authorized project

#### Scenario: Tools list has no project argument

- **WHEN** a client lists tools
- **THEN** no tool's input schema contains a `project` property
