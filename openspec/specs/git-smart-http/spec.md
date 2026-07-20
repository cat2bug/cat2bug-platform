# git-smart-http Specification

## Requirements

### Requirement: Smart HTTP clone and push via JGit

The system SHALL expose Git Smart HTTP for each project repository using in-process JGit (no server-side `git` executable and no external Git hosting process) so that authorized clients can clone, fetch, and push with a standard Git CLI.

#### Scenario: Clone with credentials

- **WHEN** a user with pull permission runs `git clone` against the repository Smart HTTP URL using platform username and password via HTTP Basic
- **THEN** the clone completes and the working copy contains the repository history available on the server

#### Scenario: Push with credentials

- **WHEN** a user with push permission runs `git push` to the repository Smart HTTP URL using HTTP Basic with valid platform credentials
- **THEN** the server accepts the push via JGit receive-pack and subsequent clones see the new commits

#### Scenario: No server-side git process

- **WHEN** Smart HTTP handles upload-pack or receive-pack
- **THEN** the server MUST NOT spawn an external `git` process to service that request

### Requirement: Reuse platform login for Git HTTP

The system SHALL authenticate Git Smart HTTP requests with HTTP Basic credentials that are validated against the same platform user authentication mechanism used for interactive login (no separate Git-only password store required for MVP).

#### Scenario: Valid login credentials accepted

- **WHEN** a Git client supplies HTTP Basic credentials that match an active platform user
- **THEN** the system authenticates the request as that user before applying repository authorization

#### Scenario: Invalid credentials rejected

- **WHEN** a Git client supplies incorrect HTTP Basic credentials
- **THEN** the system rejects the Git request without performing pack transfer

### Requirement: Pull and push role matrix

The system SHALL allow pull (upload-pack) and push (receive-pack) only for users who hold at least one of `project.create-by`, `project.admin`, `project.develop`, or `project.tester` on the repository's project. Other project members without those roles MUST be denied. Permission MUST be the union of the user's project roles.

#### Scenario: Tester can pull and push

- **WHEN** a user with only `project.tester` clones and pushes to a project repository
- **THEN** both operations succeed if credentials are valid

#### Scenario: Member without four roles denied

- **WHEN** a project member who holds none of the four roles attempts to clone
- **THEN** the system denies access

#### Scenario: Multi-role union

- **WHEN** a user holds both `project.develop` and another non-git project role
- **THEN** the user receives pull and push from the develop role

### Requirement: Force-push restricted to manage roles

The system SHALL reject non-fast-forward updates for users who lack repository manage permission, and SHALL allow force-push (non-fast-forward) only for users with `project.create-by` or `project.admin` on that project.

#### Scenario: Developer force-push denied

- **WHEN** a user with only `project.develop` attempts a non-fast-forward push
- **THEN** the system rejects the push

#### Scenario: Admin force-push allowed

- **WHEN** a project admin attempts a non-fast-forward push to a repository in their project
- **THEN** the system accepts the push

### Requirement: Commit author must match push account

The system SHALL reject receive-pack updates that introduce new commits whose author or committer identity does not match the authenticated push user. Identity match MUST accept either a case-insensitive email match against the platform user email, or a case-insensitive name match against the platform `userName` or `nickName`. Because push already requires a project role with push permission, matching the push account also ensures the commit identity is a project member.

#### Scenario: Non-member local identity rejected

- **WHEN** an authenticated project member with push permission pushes a new commit whose author name/email does not match their platform account (e.g. local `user.name=t` while logged in as `demo`)
- **THEN** the server rejects the ref update with an error explaining that author must match the push account

#### Scenario: Matching identity accepted

- **WHEN** an authenticated project member pushes a new commit whose author and committer match their platform `userName`/`nickName` or `email`
- **THEN** the server accepts the push (subject to other receive-pack rules such as fast-forward)
