## ADDED Requirements

### Requirement: Navigate from browse to branches page

The repository code browse UI SHALL provide a control (for example the displayed branch count) that navigates users with pull permission to the repository-scoped branches management page defined by `git-repo-branches`.

#### Scenario: Click branch count opens branches page

- **WHEN** a user with pull permission activates the branch-count (or equivalent) control on the code browse page
- **THEN** the UI navigates to the repository branches page
