## ADDED Requirements

### Requirement: Repository shell exposes Settings tab

The system SHALL show a Settings（管理） tab on the repository shell for users with repository manage permission, and SHALL NOT show that tab to users without manage permission.

#### Scenario: Manage user sees Settings tab

- **WHEN** a user with repository manage permission opens a repository shell page
- **THEN** the shell tabs include Settings alongside Code, Pull Request, and Releases

#### Scenario: Non-manage user does not see Settings tab

- **WHEN** a user without repository manage permission opens a repository shell page
- **THEN** the Settings tab is not shown

### Requirement: Nested settings routes under repository code path

The system SHALL serve repository settings under the SPA path `/{teamKey}/{projectKey}/code/{slug}/settings/**` nested in the repository shell. Visiting `.../settings` MUST redirect to `.../settings/general`.

#### Scenario: Settings root redirects to general

- **WHEN** a manage user navigates to `.../code/{slug}/settings`
- **THEN** the app navigates to `.../code/{slug}/settings/general` and the Settings shell tab is active

#### Scenario: Deep link to general settings

- **WHEN** a manage user opens `.../code/{slug}/settings/general`
- **THEN** the settings layout is shown with the general section selected

#### Scenario: Non-manage deep link denied

- **WHEN** a user without manage permission opens a settings URL for a repository
- **THEN** the system denies access or redirects away from settings without applying changes

### Requirement: Settings layout with sidebar navigation

The system SHALL present a settings layout with a left sidebar and a main content area. The sidebar MUST include an enabled General（基本信息） item, an enabled Delete（删除仓库） item, and a disabled Members（仓库成员管理） placeholder that cannot be activated.

#### Scenario: Sidebar shows placeholder members item

- **WHEN** a manage user opens repository settings
- **THEN** the sidebar lists General and Delete as navigable and Members as disabled (e.g. visually muted with an upcoming hint)

#### Scenario: Navigate between general and delete

- **WHEN** a manage user selects Delete in the settings sidebar
- **THEN** the main area shows the delete repository flow and the URL reflects the delete/danger settings path

### Requirement: Edit repository basic information in settings

The system SHALL allow manage users to view and update the same repository metadata fields previously available in the list settings dialog (at minimum: display name, slug, visibility, default branch, and description/remark) from `settings/general`, persisting via the existing repository update API behavior.

#### Scenario: Save basic information

- **WHEN** a manage user changes allowed basic fields and saves on the general settings page
- **THEN** the repository metadata is updated and the UI reflects the saved values

#### Scenario: Slug change updates addressability

- **WHEN** a manage user changes the repository slug from the general settings page and save succeeds
- **THEN** the repository is addressable by the new slug and the client navigates to the settings URL for the new slug

### Requirement: Delete repository from settings

The system SHALL allow manage users to delete the repository from the settings delete/danger page using the existing delete authorization and cleanup behavior, after an explicit confirmation step.

#### Scenario: Confirmed delete removes repository

- **WHEN** a manage user confirms deletion on the settings delete page
- **THEN** the repository metadata and bare repository are removed per existing delete rules and the user is returned to the project repository list

#### Scenario: Cancel leaves repository intact

- **WHEN** a manage user starts delete confirmation and cancels
- **THEN** the repository remains unchanged

### Requirement: List settings entry opens shell settings

The system SHALL navigate manage users from the project repository list settings action into the shell general settings page for that repository, and MUST NOT require a separate list-page edit dialog as the primary editor.

#### Scenario: List settings navigates to general

- **WHEN** a manage user activates Settings on a repository in the project repository list
- **THEN** the app opens `.../code/{slug}/settings/general` for that repository
