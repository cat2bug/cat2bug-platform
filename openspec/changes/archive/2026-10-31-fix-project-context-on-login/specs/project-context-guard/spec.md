## ADDED Requirements

### Requirement: Current project id must reference a membership

The system SHALL treat a user's `currentProjectId` as valid only when that user is a member of the project (`sys_user_project`) or is a documented system super-admin exempt from membership. Writing a non-zero `currentProjectId` via user-config MUST be rejected when membership (or exemption) fails. Clearing the current project (`0` / null) MUST remain allowed.

#### Scenario: Non-member cannot set current project

- **WHEN** a logged-in team member who is not in project A calls `PUT /system/user-config` with `currentProjectId` equal to project A
- **THEN** the request is rejected (403 or equivalent business error)
- **THEN** the stored `current_project_id` is unchanged

#### Scenario: Member can switch current project

- **WHEN** a project member sets `currentProjectId` to a project they belong to
- **THEN** the config is persisted and subsequent `getInfo` returns that project id

#### Scenario: Clear current project allowed

- **WHEN** a user sets `currentProjectId` to `0` or null
- **THEN** the config is persisted without requiring membership

### Requirement: GetInfo sanitizes invalid current project

On `getInfo` (and equivalent session bootstrap), if the stored `currentProjectId` is non-zero but the user is not a member (and not exempt), the system SHALL persist `currentProjectId` as cleared (`0`) and return the sanitized config.

#### Scenario: Stale project id cleared on login bootstrap

- **WHEN** a user's `sys_user_config.current_project_id` still points to project A after they are no longer a member
- **THEN** `getInfo` returns `currentProjectId` cleared
- **THEN** the database row is updated so the stale id does not return on the next bootstrap

### Requirement: Removing a project member clears their current project if matched

When a user is removed from a project, if that user's `current_project_id` equals the removed project, the system SHALL clear it to `0`.

#### Scenario: Delete member clears matching current project

- **WHEN** an admin removes user U from project A and U's `current_project_id` is A
- **THEN** U's `current_project_id` becomes `0`

#### Scenario: Delete member leaves other current project untouched

- **WHEN** an admin removes user U from project A and U's `current_project_id` is project B
- **THEN** U's `current_project_id` remains B

### Requirement: No accessible project lands on project list without project menus

After authentication bootstrap, when the user has no valid current project (`currentProjectId` empty/`0`), the UI SHALL navigate to the team project list (`/team/project-list` or the documented equivalent) unless the target route is an allowlisted non-project page (login, setup, invite, etc.). The sidebar MUST NOT show project module entries such as workbench (工作台) or defect (缺陷) while there is no valid current project.

#### Scenario: Login with no projects

- **WHEN** a user with no project memberships logs in successfully
- **THEN** they are taken to the project list page
- **THEN** the sidebar does not show workbench or defect module entries

#### Scenario: Login after invalid current project sanitized

- **WHEN** a user's only stored current project was invalid and was cleared on `getInfo`
- **THEN** they are taken to the project list page
- **THEN** the sidebar does not show workbench or defect module entries

### Requirement: Defect reads require project membership

Defect list and defect detail APIs that are scoped by `projectId` SHALL reject callers who are not members of that project (unless system super-admin exemption applies), even if the caller holds team-level menu permissions such as `system:defect:list`.

#### Scenario: Non-member cannot list defects of project A

- **WHEN** a team member who is not in project A requests the defect list with `projectId=A`
- **THEN** the request is rejected or returns no project-A data under an authorization failure
- **THEN** the caller cannot use the response to browse project A's defects

#### Scenario: Member can list defects

- **WHEN** a project member requests the defect list for their project
- **THEN** the list behaves as today under their existing functional permissions
