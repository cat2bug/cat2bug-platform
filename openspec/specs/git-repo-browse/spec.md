# git-repo-browse Specification

## Requirements

### Requirement: Browse repository contents via API

The system SHALL provide authenticated HTTP APIs for users with pull permission to list branches, list directory entries at a ref/path, read file blob content, and list a simple commit history for a project repository, without requiring a local clone.

#### Scenario: List branches

- **WHEN** a user with pull permission requests branches for a repository
- **THEN** the system returns the branch names available in that bare repository

#### Scenario: List directory

- **WHEN** a user with pull permission requests a tree path at a given ref
- **THEN** the system returns the entries (name, type, and identifiers needed to navigate) for that path

#### Scenario: Read file content

- **WHEN** a user with pull permission requests a file blob at a given ref and path
- **THEN** the system returns the file content (or a documented binary handling response) for that blob

#### Scenario: Commit log

- **WHEN** a user with pull permission requests commit history for a ref
- **THEN** the system returns a ordered list of commits with identifier, message, author, and time sufficient for a basic history view

### Requirement: Browse respects pull permission

The system SHALL deny browse APIs for users who lack pull permission on the repository's project, including system administrators who do not hold an allowed project role.

#### Scenario: Unauthorized browse denied

- **WHEN** a user without pull permission requests file content for a repository
- **THEN** the system denies the request

### Requirement: Browse UI entry in project

The system SHALL expose a project-scoped UI entry that lists accessible repositories and navigates into branch/file browse for users with pull permission, and SHALL expose manage actions only to users with manage permission.

#### Scenario: Developer can open browse UI

- **WHEN** a user with `project.develop` opens the project's code repositories area
- **THEN** they can view the repository list and browse files but do not see create/delete repository controls

#### Scenario: Admin sees manage controls

- **WHEN** a project admin opens the project's code repositories area
- **THEN** they can create, delete, and edit repository settings in addition to browsing

### Requirement: Browse aside includes releases and contributors

The project repository browse UI SHALL place a Releases section and a Contributors section in the right-hand aside below the About/intro block and above Recent activity (when Recent activity is present). Releases and Contributors behavior MUST follow `git-repo-releases` and `git-repo-contributors` respectively.

#### Scenario: Aside order on browse page

- **WHEN** a user with pull permission opens the repository code browse page
- **THEN** the right aside shows About, then Releases, then Contributors (if any), then Recent activity

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
