## Requirements

### Requirement: Remove team member without removing project membership

The system SHALL provide an authenticated API to remove a user from a team by deleting that user's `sys_user_team` row and associated team roles. The operation MUST NOT delete or alter `sys_user_project` (or project roles) for projects under that team. The team creator (`team.create-by`) MUST NOT be removable via this API. Callers MUST hold team member edit/remove permission (same family as existing team member management).

#### Scenario: Delete keeps project seats

- **WHEN** an authorized user removes a non-creator team member who also belongs to projects in that team
- **THEN** the user has no `sys_user_team` for that team
- **THEN** the user's `sys_user_project` rows for projects in that team remain unchanged

#### Scenario: Creator cannot be deleted

- **WHEN** a caller attempts to remove the user who currently holds `team.create-by` on that team
- **THEN** the system rejects the request and membership is unchanged

### Requirement: Delete confirmation mentions retained project membership

The team member management UI SHALL replace lock/unlock controls with a delete action for non-creator members. Before calling remove, the UI MUST show a confirmation that states the member will leave the team but **retain project membership** under that team when applicable.

#### Scenario: Confirm copy before delete

- **WHEN** an admin clicks delete on a non-creator member
- **THEN** a confirmation dialog appears mentioning that project membership will be kept
- **THEN** cancel leaves team membership unchanged

### Requirement: Re-add after team removal

A user who was removed from a team but may still hold project seats MUST be eligible to rejoin the team via existing invite / create-member / team-add flows, subject to team plan `max_members` quota.

#### Scenario: Re-invite after delete

- **WHEN** a removed user is invited or team-added again while under quota
- **THEN** a new `sys_user_team` row is created and they become a formal team member again

### Requirement: Team remove is independent of project member remove

Removing a user from a team MUST NOT require project-member handoff flows. Removing a user from a project MUST NOT by itself remove team membership.

#### Scenario: Team delete without project handoff

- **WHEN** a team member who is sole defect handler on a project is removed from the team only
- **THEN** team membership ends without requiring a project handoff user
- **THEN** project membership and defect handlers remain as before the team remove
