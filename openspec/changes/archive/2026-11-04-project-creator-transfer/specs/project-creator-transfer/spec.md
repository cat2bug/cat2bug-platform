## ADDED Requirements

### Requirement: Transfer project creator role

The system SHALL allow the current project creator (holder of `project.create-by`) to transfer that role to another **project member**. After a successful transfer, the project MUST have exactly one holder of `project.create-by`: the target. The previous creator MUST remain a project member without `project.create-by`, defaulting to `project.admin` when they would otherwise have no project role. The transfer MUST also set `sys_project.create_by` to the target user's id. Callers who are not the current creator MUST be rejected. Non-members MUST NOT be valid transfer targets.

#### Scenario: Successful transfer

- **WHEN** the current creator transfers to another project member
- **THEN** the target holds `project.create-by` (and no leftover ordinary-only project role clutter)
- **THEN** the previous creator no longer holds `project.create-by`, remains on the project, and has `project.admin` if they had no other project roles
- **THEN** `sys_project.create_by` equals the target user id

#### Scenario: Reject non-member target

- **WHEN** the creator chooses a user who is not a member of the project
- **THEN** the transfer is rejected and roles and `create_by` are unchanged

#### Scenario: Reject self or non-creator caller

- **WHEN** the target is the creator themself, or the caller does not hold `project.create-by`
- **THEN** the transfer is rejected

### Requirement: Creator row shows transfer instead of delete

On the project member list, the creator's row MUST NOT offer delete. Transfer SHALL be visible **only to the current project creator themself** on their own row (not to other project admins). The transfer action opens a picker of project members (excluding self), SHOULD show member nameplates, and MUST require confirmation before submit. On success the client MUST refresh session permissions/info and the member list.

#### Scenario: Creator UI

- **WHEN** the current user is the project creator and views the member list
- **THEN** their own row shows transfer and does not show delete
- **THEN** other non-creator rows keep delete (subject to remove permission and handoff rules)

#### Scenario: Non-creator cannot see transfer

- **WHEN** a non-creator (including project admin) views the member list
- **THEN** the creator row does not show transfer or delete

### Requirement: Transfer does not trigger member-remove handoff

Transferring the project creator role MUST NOT delete project membership and MUST NOT require defect or Git repository handoff.

#### Scenario: Transfer leaves membership intact

- **WHEN** a creator successfully transfers to another member
- **THEN** both users remain project members
- **THEN** open defect handlers and repository ACL rows are unchanged by the transfer itself
