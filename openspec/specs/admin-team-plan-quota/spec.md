# admin-team-plan-quota Specification

## Purpose
TBD - created by archiving change project-create-copy-and-team-quota. Update Purpose after archive.
## Requirements
### Requirement: Admin team plan module

The system SHALL provide an Admin “团队角色” (team plan) module reachable only by system super administrators (`admin` / equivalent `*:*:*`). The module SHALL allow listing, creating, updating, and (where safe) disabling team plans that define quotas.

#### Scenario: Non-admin cannot open module

- **WHEN** a user without system admin privileges navigates to the team plan Admin routes or APIs
- **THEN** access is denied

#### Scenario: Admin manages plans

- **WHEN** a system admin opens the team plan module
- **THEN** they can view and edit plan quota fields

### Requirement: Default ordinary team plan

The system SHALL seed a default plan named 「普通团队」 with:

- max team members: 10
- max projects per team: 20
- max storage per project: 1 GiB (1073741824 bytes)

Existing teams without an explicit plan binding SHALL use this default plan.

#### Scenario: Fresh install has default plan

- **WHEN** the platform is migrated/installed with this feature
- **THEN** 「普通团队」 exists with the default quota values above

### Requirement: Team binds to a plan

Each team SHALL be associated with exactly one team plan (explicit binding or default). Admin team management SHALL allow changing a team’s bound plan.

#### Scenario: Change team plan

- **WHEN** an admin assigns a different plan to a team
- **THEN** subsequent quota checks for that team use the new plan’s limits

### Requirement: Enforce max members

When adding a user to a team, the system MUST reject the operation if the resulting member count would exceed the team plan’s max members, and MUST surface a clear quota error.

#### Scenario: Invite blocked at member cap

- **WHEN** a team already has N members and the plan max members is N
- **THEN** adding another member fails with a quota error

### Requirement: Enforce max projects

When creating a project in a team, the system MUST reject the operation if the team’s project count would exceed the plan’s max projects, and MUST surface a clear quota error.

#### Scenario: Create project blocked at project cap

- **WHEN** a team already has M projects and the plan max projects is M
- **THEN** creating another project fails with a quota error

### Requirement: Enforce max project storage for copy

When starting a create-project copy whose estimated (or resulting) selected content size would exceed the plan’s per-project max storage, the system MUST reject or block that copy under quota rules defined by `project-create-copy`.

#### Scenario: Copy blocked by project storage cap

- **WHEN** the selected copy estimate exceeds the team plan’s max storage per project
- **THEN** the copy MUST NOT start until the selection fits the limit

