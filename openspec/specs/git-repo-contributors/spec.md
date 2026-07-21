# git-repo-contributors Specification

## Requirements

### Requirement: Aggregate contributors from commit authors

The system SHALL provide an authenticated API for users with pull permission that returns contributors for a repository ref, aggregated from Git commit authors. Aggregation MUST key primarily on author email when present, otherwise on author name. Each contributor entry MUST include display name, email when available, commit count, and optional matched platform user identity/avatar when the author matches a project member (or equivalent documented matching rules).

#### Scenario: Distinct authors returned with counts

- **WHEN** a user with pull permission requests contributors for the default branch of a repository that has commits from two distinct author emails
- **THEN** the system returns two contributor entries with correct commit counts

#### Scenario: Unauthorized contributors denied

- **WHEN** a user without pull permission requests contributors
- **THEN** the system denies the request

### Requirement: Contributors aside on browse page

The repository browse UI SHALL show a Contributors section below the Releases section when the contributor count is greater than zero. The section MUST show the total count, a horizontal row of up to a documented number of avatars (default 5), and an All action that navigates to or opens the full contributor list. When the count is zero, the entire Contributors section MUST be hidden.

#### Scenario: Aside shows avatars when contributors exist

- **WHEN** a user opens browse for a repository with at least one commit author
- **THEN** the Contributors aside is visible with count and avatars

#### Scenario: Aside hidden for empty history

- **WHEN** a user opens browse for an empty repository with no commits
- **THEN** the Contributors aside is not rendered

### Requirement: External contributors out of scope for this change

The system SHALL NOT require manual linking of non-project members as contributors in this change. Future changes MAY add explicit external contributor records without breaking the commit-author aggregation contract.

#### Scenario: No manual external contributor API required

- **WHEN** this change is implemented
- **THEN** there is no API requirement to attach arbitrary external people as contributors independent of commit authors
