## ADDED Requirements

### Requirement: Project member removal syncs repository ACL with optional last-admin handoff

When a user is removed from a project via the project-member removal path, the system SHALL delete that user's ACL rows on all repositories belonging to the project after any required last-admin handoff. If the departing user is the last `admin` on a repository, the removal path MUST first ensure the designated handoff user has an `admin` ACL row on that repository (insert or promote), then delete the departing user's ACL row, so the repository never has zero `admin` rows. Direct repository members-page removal of the only `admin` without this handoff path MUST continue to be rejected as today.

#### Scenario: Sync delete non-last-admin ACL on project member removal

- **WHEN** member A is successfully removed from a project and A has a non-`admin` or non-sole-`admin` ACL on project repository R
- **THEN** A's ACL row on R is deleted

#### Scenario: Last admin handed off then removed on project member removal

- **WHEN** member A is removed from a project with handoff user B and A is the only `admin` on project repository R
- **THEN** B has an `admin` ACL on R, A's ACL on R is deleted, and R still has at least one `admin`

#### Scenario: Standalone members page still rejects deleting last admin

- **WHEN** a manage user deletes the only `admin` ACL row for a repository from the repository members settings without a project-member handoff
- **THEN** the system rejects the operation and the row remains
