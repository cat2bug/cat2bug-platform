## ADDED Requirements

### Requirement: Team member removal frees max_members quota

When a user is removed from a team (formal `sys_user_team` deleted), that user MUST NOT count toward the team's plan `max_members`. A subsequent invite or team-add of any user (including the same user) MUST succeed only if the resulting formal member count would not exceed `max_members`.

#### Scenario: Seat freed after delete

- **WHEN** a team is at max members and one non-creator formal member is removed
- **THEN** inviting or team-adding another eligible user is allowed if the new count would be ≤ max_members

#### Scenario: Re-add still checks quota

- **WHEN** the team is again at max members
- **THEN** re-adding a previously removed user is rejected with a quota error
