## MODIFIED Requirements

### Requirement: Pull and push role matrix

The system SHALL allow pull (upload-pack) and push (receive-pack) according to **effective** repository permission for that repository: ACL `read`/`write`/`admin` when an ACL row exists for the user; otherwise the project-role matrix (`project.create-by`, `project.admin`, `project.develop`, `project.tester` for pull and push). Other users without effective pull/push MUST be denied. Permission MUST be the union of applicable project roles only when no ACL row exists.

#### Scenario: Tester can pull and push

- **WHEN** a user with only `project.tester` and no ACL row clones and pushes to a project repository
- **THEN** both operations succeed if credentials are valid

#### Scenario: Member without four roles denied

- **WHEN** a project member who holds none of the four roles and has no ACL row attempts to clone
- **THEN** the system denies access

#### Scenario: Multi-role union

- **WHEN** a user holds both `project.develop` and another non-git project role and has no ACL row
- **THEN** the user receives pull and push from the develop role

#### Scenario: ACL write grants non-project member push

- **WHEN** a non-project member with ACL `write` pushes with valid credentials
- **THEN** the server accepts the push subject to fast-forward rules

#### Scenario: ACL read denies push

- **WHEN** a user with ACL `read` (even if project admin) attempts to push
- **THEN** the system denies the push

### Requirement: Force-push restricted to manage roles

The system SHALL reject non-fast-forward updates for users who lack **effective** repository manage permission, and SHALL allow force-push (non-fast-forward) only for users with effective manage (ACL `admin`, or—when no ACL row—`project.create-by` or `project.admin` on that project).

#### Scenario: Developer force-push denied

- **WHEN** a user with only `project.develop` and no ACL row attempts a non-fast-forward push
- **THEN** the system rejects the push

#### Scenario: Admin force-push allowed

- **WHEN** a project admin with no demoting ACL row attempts a non-fast-forward push to a repository in their project
- **THEN** the system accepts the push

#### Scenario: ACL admin force-push allowed

- **WHEN** a user with ACL `admin` (who may not be a project admin) attempts a non-fast-forward push
- **THEN** the system accepts the push

#### Scenario: ACL read force-push denied for project admin

- **WHEN** a project admin with ACL `read` attempts a non-fast-forward push
- **THEN** the system rejects the push
