## ADDED Requirements

### Requirement: Namespaced clone URL

The system SHALL expose each repository's canonical Smart HTTP URL as `/{teamKey}/{projectKey}/code/{repoSlug}.git` (including the `.git` suffix), derived from the owning team's and project's path keys and the repository slug.

#### Scenario: Clone URL uses path keys

- **WHEN** a user with pull permission views or copies a repository clone URL
- **THEN** the URL path matches `/{teamKey}/{projectKey}/code/{repoSlug}.git`

#### Scenario: Clone via namespaced path

- **WHEN** a user with pull permission runs `git clone` against `/{teamKey}/{projectKey}/code/{repoSlug}.git` with valid HTTP Basic credentials
- **THEN** the clone succeeds using in-process JGit

### Requirement: Legacy numeric git path compatibility

The system MAY continue to accept the legacy path `/git/p/{projectId}/{slug}.git` for a documented compatibility window so existing remotes keep working, while product UI and new documentation MUST present only the namespaced URL.

#### Scenario: Legacy path still serves during compatibility window

- **WHEN** compatibility is enabled and a client uses `/git/p/{projectId}/{slug}.git` with valid credentials and permission
- **THEN** the server serves the same repository as the namespaced URL
