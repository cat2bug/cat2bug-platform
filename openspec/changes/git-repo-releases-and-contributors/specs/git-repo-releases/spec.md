## ADDED Requirements

### Requirement: Create and manage GitHub-style releases

The system SHALL allow users with repository manage permission (`project.create-by` or `project.admin`) to create, update, and delete releases for a project repository. Each release MUST be associated with a Git tag name, and MUST store a title, optional Markdown body, optional pre-release flag, optional draft flag, publisher identity, and published/updated timestamps. Creating a release MUST ensure the named tag exists in the bare repository (create the tag at the chosen commit/ref when missing; bind an existing tag when it already points at an acceptable commit).

#### Scenario: Manage user creates a published release

- **WHEN** a project admin creates a non-draft release with tag `v1.0.0`, title, and body on a commit that exists in the repository
- **THEN** the system persists the release metadata, ensures `refs/tags/v1.0.0` exists, and returns the release in subsequent list APIs for users with pull permission

#### Scenario: Developer cannot create a release

- **WHEN** a user with only `project.develop` attempts to create a release
- **THEN** the system denies the request

#### Scenario: Draft hidden from non-manage users

- **WHEN** a manage user creates a draft release and a user with only pull permission lists releases
- **THEN** the draft MUST NOT appear in that user's list

### Requirement: Release assets upload and download

The system SHALL allow manage users to upload one or more binary assets to a release, persist them under the platform local file storage, and allow users with pull permission to download assets of non-draft releases they are allowed to see. The system MUST enforce documented per-file size and per-release count limits.

#### Scenario: Upload and download asset

- **WHEN** a manage user uploads an asset to a published release and a user with pull permission downloads it
- **THEN** the download returns the uploaded file bytes with the stored content type and file name

#### Scenario: Asset on draft not downloadable by pull-only user

- **WHEN** a pull-only user attempts to download an asset belonging to a draft release
- **THEN** the system denies the request

### Requirement: Browse page releases aside

The repository browse UI SHALL show a Releases section below the About/intro block. The section MUST always be visible. When there are no visible releases for the current user, the UI MUST show an empty-state message that includes a Create action for manage users (Create MAY be omitted or disabled for non-manage users).

#### Scenario: Empty releases aside with create for admin

- **WHEN** a project admin opens a repository browse page that has no releases
- **THEN** the Releases aside shows empty-state text and a Create control that opens the create-release flow

#### Scenario: Pull-only user sees empty state without create

- **WHEN** a pull-only user opens a repository with no published releases
- **THEN** the Releases aside is still shown with empty-state text and without a working Create action
