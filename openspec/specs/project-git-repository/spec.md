# project-git-repository Specification

## Purpose

Allow a project to own zero or more Git repositories with metadata, bare storage, and permission checks based on effective repository access (ACL override, else project roles).

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

The system SHALL restrict repository lifecycle operations (create, delete, rename slug, change disk path, and allowing force-push) to users who have **effective** repository manage permission on the target repository: either an ACL role of `admin` on that repository, or—when the user has no ACL row for that repository—`project.create-by` or `project.admin` on the owning project (permission union if multiple project roles). Users who only hold team-admin or system-admin without those effective rights MUST be denied. When an ACL row exists for the user, that row MUST fully override project roles for that repository.

#### Scenario: Project admin can create repository

- **WHEN** a project admin creates a repository in their project
- **THEN** the operation succeeds and the creator receives an ACL `admin` row

#### Scenario: Developer cannot create repository

- **WHEN** a user who only has `project.develop` attempts to create a repository
- **THEN** the system denies the operation

#### Scenario: System admin does not bypass

- **WHEN** a system administrator who is not assigned a managing project role on the target project and has no repository ACL `admin` attempts to delete a repository
- **THEN** the system denies the operation

#### Scenario: ACL demotion blocks lifecycle manage

- **WHEN** a project admin has an ACL `read` row on a repository and attempts to delete that repository
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

### Requirement: Create entry uses unified multi-mode dialog

The system SHALL open the same unified create-or-import dialog for repository creation from both the project repository list “new” action and the in-browse repository switcher add action. Empty-repository creation MUST remain available as one mode of that dialog and MUST continue to require repository manage permission.

#### Scenario: List new opens unified dialog

- **WHEN** a manage user clicks new repository on the project repository list
- **THEN** the unified dialog opens with mode switching including empty and import modes

#### Scenario: Switcher add opens unified dialog

- **WHEN** a manage user clicks add in the repository switcher popover
- **THEN** the same unified dialog capability opens (not a divergent create-only form)

#### Scenario: Non-manage user cannot open create

- **WHEN** a user without repository manage permission uses the UI
- **THEN** the new/add create controls are not offered (or the operation is denied)
