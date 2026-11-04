## ADDED Requirements

### Requirement: Preview impact before removing a project member

The system SHALL provide a preview for removing a project member that reports whether a handoff user is required and summarizes affected open defects and Git repositories. Open defects are those whose state is not `RESOLVED` and not `CLOSED`. A sole-handler defect is an open defect whose `handle_by` contains exactly one user id equal to the member being removed. A multi-handler defect is an open defect whose `handle_by` contains the member and at least one other user id. A last-admin repository is a repository in the same project where the member has an `admin` ACL row and no other user has `admin` on that repository. `requiresHandoff` MUST be true when there is at least one sole-handler defect or at least one last-admin repository.

#### Scenario: Preview requires handoff for sole handler

- **WHEN** an authorized user previews removing member A who is the sole handler of one or more open defects
- **THEN** the preview returns `requiresHandoff` true and lists those defects

#### Scenario: Preview requires handoff for last admin only

- **WHEN** an authorized user previews removing member A who has no sole-handler open defects but is the last `admin` on at least one project repository
- **THEN** the preview returns `requiresHandoff` true and lists those repositories

#### Scenario: Preview does not require handoff for multi-handler only

- **WHEN** an authorized user previews removing member A who only appears on open defects with multiple handlers and is not last admin on any project repository
- **THEN** the preview returns `requiresHandoff` false

### Requirement: Remove project member with unified handoff when required

When `requiresHandoff` would be true for the target member, the system SHALL require a `handoffUserId` that identifies a different current project member. On success the system MUST, in one transaction: reassign every sole-handler open defect so `handle_by` becomes exactly `[handoffUserId]` without changing defect state; remove the departing member from every multi-handler open defect `handle_by`; for each last-admin repository ensure the handoff user has an `admin` ACL row then delete the departing member's ACL row; delete the departing member's ACL rows on other project repositories; then delete the project membership. The system MUST reject the removal when handoff is required but `handoffUserId` is missing, invalid, equal to the departing member, or not a project member.

#### Scenario: Handoff reassigns sole-handler defects and transfers last admin

- **WHEN** an authorized user removes member A with handoff user B while A is sole handler of open defects and last admin of repository R
- **THEN** those defects have `handle_by` equal to `[B]`, B is `admin` on R, A's ACL on R is gone, and A is no longer a project member

#### Scenario: Removal without handoff when not required

- **WHEN** an authorized user removes member A and preview would set `requiresHandoff` false
- **THEN** the system removes A from multi-handler open defects and from non-last-admin repository ACL rows, deletes the project membership, and does not require `handoffUserId`

#### Scenario: Reject when handoff required but omitted

- **WHEN** an authorized user attempts to remove member A without `handoffUserId` while A is sole handler of an open defect or last admin of a repository
- **THEN** the system rejects the request and leaves membership, defects, and ACL unchanged

### Requirement: Project member remove handoff UI

The project member management page SHALL call preview before deletion. When `requiresHandoff` is false, it MAY keep a simple confirm dialog. When `requiresHandoff` is true, it MUST show a dialog that lists affected sole-handler defects and last-admin repositories (at least counts and identifying labels), requires selecting one handoff project member other than the departing member, and submits removal with that handoff user. Cancel MUST leave membership unchanged.

#### Scenario: User confirms handoff and removal

- **WHEN** the user selects a handoff member and confirms in the handoff dialog
- **THEN** the client sends the removal including that handoff user and refreshes the member list on success

#### Scenario: User cancels handoff dialog

- **WHEN** the user cancels the handoff dialog
- **THEN** the departing user remains a project member
