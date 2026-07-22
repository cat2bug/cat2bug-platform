## ADDED Requirements

### Requirement: One-shot import from remote Git URL

The system SHALL allow a user with repository manage permission to import an existing remote Git repository into a project as a local bare repository in a single operation. After a successful import, the hosted repository MUST NOT depend on the remote for normal browse/clone/push (one-shot; no continuous mirror obligation). Credentials supplied for the import MUST NOT be persisted to the database or configuration files.

#### Scenario: Import public HTTPS URL without token

- **WHEN** a manage user submits an import with a reachable public HTTPS Git URL, a unique slug, and other required metadata for a project
- **THEN** the system creates a bare repository on disk containing the remote history, persists repository metadata, and the repository appears in the project repository list

#### Scenario: Import private URL with ephemeral token

- **WHEN** a manage user submits an import with a private HTTPS Git URL and a username/token in the same request
- **THEN** the system uses those credentials only for that request to clone, does not store them after completion, and on success exposes the repository like a normally created project repository

#### Scenario: Failed import leaves no list entry

- **WHEN** an import fails (invalid URL, auth failure, clone error, or timeout)
- **THEN** the system does not leave a visible repository row for that attempt and MUST NOT leave an unusable bare directory for that slug (cleanup or never-create)

#### Scenario: Success before list visibility

- **WHEN** an import is still in progress
- **THEN** other users listing project repositories MUST NOT see a repository for that import until metadata has been successfully committed after clone

### Requirement: Provider-assisted repository picking via session PAT

The system SHALL support listing remote repositories from GitHub, GitLab, and Gitee using a personal access token (or equivalent) supplied for the current session/request, so the user can pick a repository and obtain its clone URL for import. The system MUST NOT require a stored OAuth app binding for this listing.

#### Scenario: List GitHub repositories with PAT

- **WHEN** a manage user provides a GitHub PAT and requests a remote repository list for provider GitHub
- **THEN** the system returns repositories visible to that token (name and clone URL at minimum) without persisting the PAT

#### Scenario: List Gitee repositories with PAT

- **WHEN** a manage user provides a Gitee PAT and requests a remote repository list for provider Gitee
- **THEN** the system returns repositories visible to that token without persisting the PAT

#### Scenario: List GitLab repositories with configurable host

- **WHEN** a manage user provides a GitLab PAT and a host URL (defaulting to `https://gitlab.com` when omitted) and requests a remote repository list for provider GitLab
- **THEN** the system calls that host’s API with the PAT and returns list results without persisting the PAT or host credentials

#### Scenario: Invalid or unauthorized PAT

- **WHEN** the provider API rejects the supplied PAT
- **THEN** the system returns an error suitable for UI display and does not create a local repository

### Requirement: Unified create-or-import dialog modes

The system SHALL present a single create dialog with mutually exclusive modes: empty repository, import from URL, import from GitHub, import from GitLab, and import from Gitee. The project repository list “new” action and the repository switcher “add” action MUST open this same dialog capability.

#### Scenario: Empty mode still creates blank bare repo

- **WHEN** a manage user chooses empty mode and submits valid name and slug
- **THEN** the system creates an empty bare repository as today (no remote clone)

#### Scenario: Switching mode clears secrets and source selection

- **WHEN** a user switches from a provider or URL mode to another mode
- **THEN** the UI MUST clear token/password fields and prior remote selection for the previous mode (shared name/slug fields MAY be retained)

#### Scenario: Provider pick prefills clone target fields

- **WHEN** a user selects a remote repository from a provider list
- **THEN** the dialog uses that repository’s clone URL for the subsequent import and MAY suggest name/slug/default branch when those fields are still empty or auto-filled
