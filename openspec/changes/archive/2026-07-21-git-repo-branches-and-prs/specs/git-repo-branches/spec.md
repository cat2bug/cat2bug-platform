## ADDED Requirements

### Requirement: Repository branch management page

The system SHALL provide a repository-scoped branch management UI (independent route, not only the code-browse branch dropdown) for users with pull permission. The page MUST show a default-branch section and an all-branches section, support filtering branches by name search, and for each listed branch show last-update information (tip commit author and time when available) and ahead/behind counts relative to the repository default branch.

#### Scenario: Open branches page

- **WHEN** a user with pull permission opens the repository branches page
- **THEN** the UI shows the default branch (when one exists) and the full branch list with search

#### Scenario: Search by name

- **WHEN** the user enters a branch name query on the branches page
- **THEN** the list shows only branches whose names match the query

#### Scenario: Ahead behind vs default

- **WHEN** a non-default branch is listed and the default branch exists
- **THEN** the UI shows how many commits the branch is ahead of and behind the default branch

### Requirement: Enhanced branch list API

The system SHALL extend the authenticated branch list API for users with pull permission so that each branch includes at least: name, whether it is the default branch, tip commit identity fields needed for last-update display (when the branch has commits), and ahead/behind counts relative to the default branch when both tips resolve.

#### Scenario: Pull user lists enriched branches

- **WHEN** a user with pull permission requests the branch list for a non-empty repository
- **THEN** the response includes default flag, last-update fields when available, and ahead/behind relative to the default branch

#### Scenario: Empty repository

- **WHEN** a repository has no branch refs
- **THEN** the API returns an empty list without error

### Requirement: Manage creates and deletes branches

The system SHALL allow users with repository manage permission (`project.create-by` or `project.admin`) to create a branch from a specified existing ref (defaulting to the current default branch tip) and to delete a non-default branch via authenticated APIs and the branches UI. Users who only have push permission MUST be denied these UI/API write operations (they may still create remote branches via Git Smart HTTP push as already specified elsewhere).

#### Scenario: Admin creates branch

- **WHEN** a project admin creates a branch named `feature/x` from the default branch tip
- **THEN** `refs/heads/feature/x` exists and appears on the branches page

#### Scenario: Developer denied create via API

- **WHEN** a user with only `project.develop` calls the create-branch API
- **THEN** the system denies the operation

#### Scenario: Cannot delete default branch

- **WHEN** a manage user attempts to delete the current default branch
- **THEN** the system rejects the deletion

### Requirement: Manage sets default branch

The system SHALL allow manage users to set the repository default branch to an existing local branch name, persisting it in repository metadata used by browse and clone defaults.

#### Scenario: Change default branch

- **WHEN** a manage user sets the default branch from `main` to `core` and `core` exists
- **THEN** subsequent branch list and browse defaults treat `core` as the default branch

### Requirement: Create PR entry from branches page

The branches page SHALL offer a control to start a Pull Request with the selected branch as source and the repository default branch as the suggested target, for users who have permission to create pull requests as defined in `git-repo-pull-requests`.

#### Scenario: Open create PR from branch row

- **WHEN** a user with push permission activates create-PR on branch `feature/x` whose default target is `core`
- **THEN** the create-PR flow opens with source `feature/x` and target `core` prefilled
