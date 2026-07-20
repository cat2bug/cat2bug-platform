# project-git-repository Specification

## Requirements

### Requirement: Project has multiple git repositories

The system SHALL allow a project to own zero or more code repositories ("工程"), each backed by a Git bare repository on disk and a metadata record linking it to the project.

#### Scenario: Create repository under project

- **WHEN** a user with repository manage permission creates a repository with a display name and slug for a project
- **THEN** the system persists metadata for that repository and initializes a bare Git repository on disk at the configured path

#### Scenario: List repositories for project

- **WHEN** a user with repository pull permission requests the repository list for a project
- **THEN** the system returns only repositories belonging to that project that the user is allowed to access under the git permission matrix

#### Scenario: Multiple repositories per project

- **WHEN** manage users create two repositories with distinct slugs under the same project
- **THEN** both repositories exist independently and each exposes its own clone URL

### Requirement: Repository manage permission

The system SHALL restrict repository lifecycle operations (create, delete, rename slug, change disk path, and allowing force-push) to users who hold `project.create-by` or `project.admin` on that project (permission union if multiple roles). Users who only hold team-admin or system-admin without those project roles MUST be denied.

#### Scenario: Project admin can create repository

- **WHEN** a project admin creates a repository in their project
- **THEN** the operation succeeds

#### Scenario: Developer cannot create repository

- **WHEN** a user who only has `project.develop` attempts to create a repository
- **THEN** the system denies the operation

#### Scenario: System admin does not bypass

- **WHEN** a system administrator who is not assigned a managing project role on the target project attempts to delete a repository
- **THEN** the system denies the operation

### Requirement: Slug may change after create

The system SHALL allow manage users to change a repository slug after creation, update stored metadata (and disk location as designed), and surface that previous clone URLs based on the old slug are no longer valid.

#### Scenario: Rename slug

- **WHEN** a manage user changes a repository slug from `api` to `api-v2`
- **THEN** the repository is addressable by the new slug and the UI or API response indicates the clone URL changed

### Requirement: Disk path configuration

The system SHALL support a default disk path under `cat2bug.profile` and SHALL allow manage users to set an absolute disk path, which MUST be normalized and validated for writability before use.

#### Scenario: Default path when omitted

- **WHEN** a repository is created without an explicit disk path
- **THEN** the bare repository is created under the configured profile git root for that project and slug

#### Scenario: Absolute path accepted when valid

- **WHEN** a manage user sets a writable absolute path for a repository
- **THEN** the system stores that path and uses it for the bare repository

#### Scenario: Invalid path rejected

- **WHEN** a manage user sets a path that fails normalization or writability checks
- **THEN** the system rejects the change and does not move or initialize the repository at that path
