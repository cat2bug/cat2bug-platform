## ADDED Requirements

### Requirement: Project git overview API

The system SHALL expose a project-scoped overview endpoint that returns, for the repositories the caller is allowed to see in that project: repository count, open pull-request count, collect (follow) row count, and a light language distribution.

#### Scenario: Authorized caller receives aggregates

- **WHEN** a user with permission to list at least one (or zero) git repositories in a project requests the project git overview
- **THEN** the system returns `repoCount` equal to the number of repositories visible to that user in the project, `openPrCount` equal to the number of pull requests in state `open` on those repositories, and `collectCount` equal to the number of `sys_user_git_repository` rows with collect enabled for those repositories (summed across users)

#### Scenario: Unauthorized caller denied

- **WHEN** a user without project git list/pull access for the project requests the overview
- **THEN** the system denies the request

#### Scenario: Empty project

- **WHEN** an authorized user requests overview for a project with no visible repositories
- **THEN** `repoCount`, `openPrCount`, and `collectCount` are zero and `languages` is an empty list

### Requirement: Collect count is project-wide follows

The system SHALL compute `collectCount` as the total number of follow/collect records for visible repositories in the project, not as the count of distinct users and not limited to the current user's follows.

#### Scenario: Multiple users following same repository

- **WHEN** two users each have collect enabled on the same visible repository and no other collects exist
- **THEN** `collectCount` is 2

### Requirement: Light language distribution by extension

The system SHALL estimate language share by mapping file path extensions on each visible repository's default branch to language labels, weighting by file count, applying per-repository and per-project scan caps, skipping common generated/vendor path segments, merging across repositories, and returning a top list plus an "other" bucket with percentages.

#### Scenario: Dominant extensions surface in ranking

- **WHEN** visible repositories' default branches contain more `.java` files than other mapped extensions within scan caps
- **THEN** the languages list includes an entry whose name corresponds to Java with the highest percent among returned languages

#### Scenario: Scan failure on one repository does not fail overview

- **WHEN** one visible repository cannot be opened or has no default branch while others succeed
- **THEN** the overview still returns HTTP success with stats for all visible repositories and languages aggregated from the repositories that scanned successfully

#### Scenario: Unknown extensions roll into other

- **WHEN** scanned files include extensions not in the built-in mapping
- **THEN** those files contribute to an "other" (或其他 locale label) bucket in the returned distribution

### Requirement: Repo list page overview aside

The system SHALL show a right-hand overview aside on the project source-management repository list page on wide viewports, displaying the three stats (repository count, open PR count, collect count), the language distribution bars, and a project members preview; the aside MUST NOT include a Fork metric.

#### Scenario: Wide viewport shows aside

- **WHEN** a user opens the project git repository list on a viewport width of at least 1200px
- **THEN** the page shows the existing list and a right aside with repository, PR, and collect stats, language section, and members section

#### Scenario: Narrow viewport hides aside

- **WHEN** a user opens the same list on a viewport width below 1200px
- **THEN** the overview aside is not shown and the list remains usable

#### Scenario: Collect toggle updates aside count

- **WHEN** the user enables or disables collect on a repository from the list while the aside is visible
- **THEN** the aside collect count increases or decreases by one without requiring a full page reload

### Requirement: Members preview uses project members

The overview aside SHALL display members of the current project (avatar and display name) with a heading that includes the member count; it MUST NOT invent a separate git-only member census for this panel.

#### Scenario: Project members appear in aside

- **WHEN** the project has two members and the aside is shown
- **THEN** the members section title reflects count 2 and both members are listed with name (and avatar when available)
