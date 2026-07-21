## ADDED Requirements

### Requirement: Commit detail API with diff limits

The system SHALL provide an authenticated API for users with pull permission to retrieve a single commit by id (full or repository-resolvable abbreviated form) for a project repository. The response MUST include the full commit message, author identity fields, commit time, parent commit identifiers, aggregate change statistics (file count and addition/deletion line totals when available), and a list of changed files. For each changed file the system MUST include path, change type, and whether the content is treated as binary or truncated. For non-binary text files within documented size limits, the system MUST include a unified diff patch; binary files and patches exceeding the documented per-file or cumulative limits MUST omit the full patch and indicate truncated or binary status. Access MUST use the same pull permission rules as other browse APIs.

#### Scenario: Pull user loads commit detail with text diff

- **WHEN** a user with pull permission requests commit detail for an existing commit that changes a small text file
- **THEN** the system returns full message, parents, file list with stats, and a unified patch for that text file

#### Scenario: Binary or oversized patch omitted

- **WHEN** a changed file is binary or its patch would exceed the documented size limit
- **THEN** the system returns the file entry without a full patch and marks it as binary or truncated

#### Scenario: Unauthorized commit detail denied

- **WHEN** a user without pull permission requests commit detail
- **THEN** the system denies the request

#### Scenario: Unknown commit

- **WHEN** a user with pull permission requests a commit id that does not resolve in the repository
- **THEN** the system returns a not-found error

### Requirement: Commit detail page and navigation entries

The repository UI SHALL provide a commit detail view reachable at a documented route under the repository browse area. The view MUST show the commit subject and body (from the full message), author (with avatar when matched to a project member using the same rules as commit lists), relative or absolute time, parent links when parents exist, aggregate file-change summary, and per-file unified diffs for files that include a patch. The UI MUST offer a control to browse the repository tree at that commit. The following entry points MUST navigate to the same detail view when activated: the commit message on the commits tab list, the latest-commit message on the code tab, and the recent-activity message or commit hash in the browse aside.

#### Scenario: Open detail from commits tab

- **WHEN** a user with pull permission clicks a commit message on the commits tab
- **THEN** the UI navigates to the commit detail view for that commit

#### Scenario: Open detail from code tab latest commit

- **WHEN** a user clicks the latest-commit message on the code browse tab
- **THEN** the UI navigates to the commit detail view for that commit

#### Scenario: Open detail from aside recent activity

- **WHEN** a user clicks the message or hash of an item in the aside recent activity list
- **THEN** the UI navigates to the commit detail view for that commit

#### Scenario: Browse files at commit

- **WHEN** a user activates browse-files on the commit detail view
- **THEN** the UI opens the code browse experience with that commit as the current ref
