## ADDED Requirements

### Requirement: Transfer team creator role

The system SHALL allow the current team creator to transfer the `team.create-by` role to another **formal** team member (user with `sys_user_team` on that team). After a successful transfer, the team MUST have exactly one holder of `team.create-by`: the target. The previous creator MUST remain a formal team member without `team.create-by`, defaulting to `team.admin` when they would otherwise have no team role. Project-only affiliates (no `sys_user_team`) MUST NOT be valid transfer targets.

#### Scenario: Successful transfer

- **WHEN** the current creator transfers to another formal teammate
    - **THEN** the target holds `team.create-by` (and no leftover ordinary-only role clutter)
    - **THEN** the previous creator no longer holds `team.create-by`, remains on the team, and has `team.admin` if they had no other team roles

#### Scenario: Reject project-only target

- **WHEN** the creator chooses a user who is only a project member of the team and has no `sys_user_team`
- **THEN** the transfer is rejected and roles are unchanged

#### Scenario: Reject self or non-member

- **WHEN** the target is the creator themself or is not a formal team member
- **THEN** the transfer is rejected

### Requirement: Creator row shows transfer instead of delete

On the team member list, the creator's row MUST NOT offer delete. Transfer SHALL be visible **only to the current team creator themself** on their own row (not to other admins). The transfer action opens a picker limited to formal teammates (excluding self) and MUST require confirmation before submit.

#### Scenario: Creator UI

- **WHEN** the current user is the team creator and views the member list
- **THEN** their own row shows transfer and does not show delete
- **THEN** other non-creator rows show delete and do not show transfer

#### Scenario: Non-creator cannot see transfer

- **WHEN** a non-creator (including team admin) views the member list
- **THEN** the creator row does not show transfer or delete
