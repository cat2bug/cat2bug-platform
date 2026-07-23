## ADDED Requirements

### Requirement: Repository tags management page

The system SHALL provide a repository-scoped Tags page at the code route `code/{slug}/tags` for users with pull permission. The page MUST list Git tags with at least: tag name, description (annotated tag message when present), and tip commit summary (short id and commit subject/time when available). The page MUST support filtering tags by a search query matching tag name (and MAY also match message). Manage users MUST be able to open a create dialog (`el-dialog`) and delete tags from the list. Each row MUST offer download of the tag’s source archive, and either a create-release action (when no release exists for that tag) or navigation to the existing release (when one exists).

#### Scenario: Open tags page

- **WHEN** a user with pull permission opens the repository tags page
- **THEN** the UI lists existing tags with name, description when available, and tip commit summary

#### Scenario: Search by name

- **WHEN** the user enters a search query on the tags page
- **THEN** the list shows only tags whose names match the query (case-insensitive substring matching is allowed)

#### Scenario: Create via dialog

- **WHEN** a manage user opens create tag and submits a valid name, optional description, and target ref/commit
- **THEN** the dialog closes and the new tag appears in the list

#### Scenario: Download archive

- **WHEN** a user with pull permission activates download on tag `v1.0.0`
- **THEN** the system provides an archive of the repository at that tag ref

#### Scenario: Create release from tag without release

- **WHEN** a manage user activates create release on a tag that has no associated release
- **THEN** the create-release flow opens with that tag name prefilled

### Requirement: Tag list API

The system SHALL expose an authenticated tag list API for users with pull permission. Each item MUST include: tag name, whether the tag is annotated, message (empty when absent), peeled commit id, and fields needed for tip commit display when resolvable. When a `sys_git_release` row exists for the same repository and tag name, the item MUST include that release’s identity so the UI can distinguish create-release vs open-release. An optional query parameter MUST filter results by name substring.

#### Scenario: Pull user lists tags

- **WHEN** a user with pull permission requests the tag list for a repository that has tags
- **THEN** the response includes name, annotation/message fields, peel commit identity, and release id when a release is bound to the tag

#### Scenario: Empty repository tags

- **WHEN** a repository has no tag refs
- **THEN** the API returns an empty list without error

#### Scenario: Filter by query

- **WHEN** a pull user requests the tag list with `q=v1`
- **THEN** only tags whose names contain `v1` (case-insensitive) are returned

### Requirement: Manage creates annotated tags

The system SHALL allow users with repository manage permission (`project.create-by` or `project.admin`) to create a Git tag via API and the Tags UI. Tags created through this capability MUST be annotated tags whose message is the provided description (empty message allowed). The target MUST resolve from an explicit commit id or ref (branch or other resolvable ref). Creating a tag whose name already exists MUST be rejected. Users without manage permission MUST be denied.

#### Scenario: Admin creates annotated tag

- **WHEN** a project admin creates tag `v2.0.0` with description `First stable` pointing at an existing commit
- **THEN** `refs/tags/v2.0.0` exists as an annotated tag whose message is `First stable` and peels to that commit

#### Scenario: Duplicate tag rejected

- **WHEN** a manage user attempts to create a tag name that already exists
- **THEN** the system rejects the request without modifying the existing tag

#### Scenario: Developer denied create

- **WHEN** a user with only `project.develop` calls the create-tag API
- **THEN** the system denies the operation

### Requirement: Manage deletes tags with release cascade

The system SHALL allow manage users to delete a Git tag. When no release is associated with the tag name, the system MUST delete only `refs/tags/<name>`. When a release is associated with the tag name, the client MUST supply an explicit cascade flag; without the flag the system MUST reject the delete with a conflict indicating a release exists. With the cascade flag, the system MUST delete the associated release using the same asset and database cleanup as deleting that release normally, and MUST then delete the Git tag. Deleting a release through the releases API MUST continue to retain the Git tag (unchanged behavior).

#### Scenario: Delete tag without release

- **WHEN** a manage user deletes tag `v0.1.0` that has no associated release
- **THEN** `refs/tags/v0.1.0` is removed and no release rows are affected

#### Scenario: Delete tag with release requires cascade flag

- **WHEN** a manage user deletes tag `v1.0.0` that has an associated release without the cascade flag
- **THEN** the system rejects the request and the tag and release remain

#### Scenario: Cascade delete tag and release

- **WHEN** a manage user deletes tag `v1.0.0` that has an associated release with the cascade flag set
- **THEN** the release and its assets are removed and `refs/tags/v1.0.0` is removed

#### Scenario: UI confirms cascade

- **WHEN** a manage user chooses delete on a tag that has an associated release
- **THEN** the UI requires confirmation that the release will also be deleted before calling the API with the cascade flag
