## ADDED Requirements

### Requirement: Browse repository contents via API

The system SHALL provide authenticated HTTP APIs for users with pull permission to list branches, list directory entries at a ref/path, read file blob content, and list a simple commit history for a project repository, without requiring a local clone.

#### Scenario: List branches

- **WHEN** a user with pull permission requests branches for a repository
- **THEN** the system returns the branch names available in that bare repository

#### Scenario: List directory

- **WHEN** a user with pull permission requests a tree path at a given ref
- **THEN** the system returns the entries (name, type, and identifiers needed to navigate) for that path

#### Scenario: Read file content

- **WHEN** a user with pull permission requests a file blob at a given ref and path
- **THEN** the system returns the file content (or a documented binary handling response) for that blob

#### Scenario: Commit log

- **WHEN** a user with pull permission requests commit history for a ref
- **THEN** the system returns a ordered list of commits with identifier, message, author, and time sufficient for a basic history view

### Requirement: Browse respects pull permission

The system SHALL deny browse APIs for users who lack pull permission on the repository's project, including system administrators who do not hold an allowed project role.

#### Scenario: Unauthorized browse denied

- **WHEN** a user without pull permission requests file content for a repository
- **THEN** the system denies the request

### Requirement: Browse UI entry in project

The system SHALL expose a project-scoped UI entry that lists accessible repositories and navigates into branch/file browse for users with pull permission, and SHALL expose manage actions only to users with manage permission.

#### Scenario: Developer can open browse UI

- **WHEN** a user with `project.develop` opens the project's code repositories area
- **THEN** they can view the repository list and browse files but do not see create/delete repository controls

#### Scenario: Admin sees manage controls

- **WHEN** a project admin opens the project's code repositories area
- **THEN** they can create, delete, and edit repository settings in addition to browsing
