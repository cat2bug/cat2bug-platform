# git-repo-pull-requests Specification

## Requirements

### Requirement: Create and list pull requests

The system SHALL allow users with push permission (`project.create-by`, `project.admin`, `project.develop`, or `project.tester`) to create a pull request for a project repository with title, optional description, source branch, and target branch. Users with pull permission SHALL be able to list pull requests for that repository. Each pull request MUST have a repository-scoped sequential number and a state among open, merged, and closed.

#### Scenario: Developer creates PR

- **WHEN** a user with `project.develop` creates a PR from `feature/x` into the default branch with a title
- **THEN** the PR is stored as open with a new number and appears in the repository PR list

#### Scenario: Member without push denied create

- **WHEN** a project member without push permission attempts to create a PR
- **THEN** the system denies the operation

#### Scenario: Pull user lists PRs

- **WHEN** a user with pull permission requests the PR list
- **THEN** the system returns pull requests for that repository

### Requirement: Pull request detail with commits and files

The system SHALL provide an authenticated detail view/API for users with pull permission that includes PR metadata, the list of commits reachable from source but not target, and a changed-file list with diffs subject to the same binary/size truncation rules as commit detail browse.

#### Scenario: Open PR shows commits and files

- **WHEN** a user with pull permission opens an open PR that has commits ahead of the target
- **THEN** the system returns those commits and the changed files with patches when within limits

### Requirement: Whole-PR discussion comments

The system SHALL allow users with push permission to add discussion comments on a pull request (whole-PR comments, not requiring line anchors). Users with pull permission SHALL be able to read the comment thread. Optional replies to a parent comment MAY be supported. Inline/file-line comments are NOT required in this capability.

#### Scenario: Comment on open PR

- **WHEN** a user with push permission posts a comment body on an open PR
- **THEN** the comment appears in the PR discussion for users with pull permission

#### Scenario: Pull-only cannot comment

- **WHEN** a user with only pull permission (without push) attempts to post a PR comment
- **THEN** the system denies the operation

### Requirement: Merge pull request restricted to manage

The system SHALL allow only users with repository manage permission to merge an open pull request into its target branch using a server-side merge that produces a merge commit when histories have diverged (or a fast-forward when possible under the chosen merge implementation), updates the target ref on success, and marks the PR merged. On merge conflicts the system MUST reject the merge without updating the target ref and MUST surface an error suitable for the UI.

#### Scenario: Admin merges clean PR

- **WHEN** a project admin merges an open PR whose source can merge cleanly into the target
- **THEN** the target branch tip advances, the PR state becomes merged, and subsequent fetches see the merge result

#### Scenario: Developer denied merge

- **WHEN** a user with only `project.develop` attempts to merge a PR
- **THEN** the system denies the operation

#### Scenario: Conflict rejects merge

- **WHEN** merging would conflict
- **THEN** the target ref is unchanged and the PR remains open

### Requirement: Close pull request

The system SHALL allow the PR author (with push permission) or a manage user to close an open pull request without merging. Closed PRs MUST NOT be mergeable unless reopened by a documented reopen API (reopen is optional for MVP; if absent, closed stays closed).

#### Scenario: Author closes own PR

- **WHEN** the author of an open PR closes it
- **THEN** the PR state becomes closed and merge is no longer offered

#### Scenario: Admin closes any open PR

- **WHEN** a manage user closes another user's open PR
- **THEN** the PR state becomes closed

### Requirement: Pull request UI routes

The system SHALL expose repository-scoped UI for creating a PR, listing PRs, and viewing PR detail (metadata, commits/files, discussion, merge/close actions per permission). Navigation from the branches page create-PR control MUST land in the create flow with source/target prefills when provided.

#### Scenario: Navigate to PR detail

- **WHEN** a user with pull permission opens a PR from the list
- **THEN** the detail UI shows metadata, discussion, and available actions for that user's permissions
