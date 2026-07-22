## MODIFIED Requirements

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
