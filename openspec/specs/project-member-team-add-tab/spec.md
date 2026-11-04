## Requirements

### Requirement: Team-not-member list API for a project

The system SHALL provide `GET /system/project/{projectId}/team-not-member` (or equivalent documented path) that returns users who are members of the project's team and are not members of the project. Empty or blank search MUST still return a paginated list of such users (unlike the global not-member endpoint). Optional search MUST filter by nick name, phone number, or email (fuzzy). Locked team membership and locked project membership rows MUST be treated as non-members for inclusion/exclusion respectively. The endpoint MUST require project member-add permission.

#### Scenario: Empty search lists team users not in project

- **WHEN** an authorized user requests team-not-member for a project with no search keyword
- **THEN** the response contains team members who are not yet project members (paginated)

#### Scenario: Search narrows team-not-member results

- **WHEN** an authorized user requests team-not-member with a nick-name fragment matching a team user who is not in the project
- **THEN** that user appears in the results

#### Scenario: Existing project members excluded

- **WHEN** a user is both a team member and a project member
- **THEN** that user does not appear in team-not-member results

### Requirement: Add-member dialog Team tab is first and default

The project add-member dialog SHALL include a「团队添加」tab as the first tab, before「搜索添加」and「邀请链接」. Opening the dialog MUST activate the Team tab by default. The Team tab MUST show a multi-select of project roles (excluding project-creator roles, same as direct add) and a checklist of candidates from team-not-member. The dialog footer submit control MUST be available on the Team tab.

#### Scenario: Open dialog lands on Team tab

- **WHEN** a user with add-member permission opens the add-member dialog
- **THEN** the Team tab is selected first

#### Scenario: Empty team pool shows empty state

- **WHEN** every team member is already on the project (or the team has no eligible users)
- **THEN** the Team tab shows an empty state and does not invent global users

### Requirement: Team tab submit uses existing project member add

Submitting the Team tab MUST call the existing project batch-add member API with the selected `memberIds` and `roleIds`. Success MUST add those users as project members with the chosen roles and MUST NOT create or alter `sys_user_team` rows. Missing roles or empty member selection MUST be rejected with validation feedback. Direct-add and invite-link tabs MUST keep their existing behavior, including the global not-member empty-search ban.

#### Scenario: Batch add from Team tab

- **WHEN** the user selects at least one role and one or more team-not-member users and submits
- **THEN** those users become project members with the selected roles and remain team members as before

#### Scenario: Reject Team tab submit without roles

- **WHEN** the user selects members but no roles and submits on the Team tab
- **THEN** the add fails or is blocked by validation and no membership is created
