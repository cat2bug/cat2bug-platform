## ADDED Requirements

### Requirement: Team settings host case and defect configuration

The team settings surface SHALL provide catalog entries (cards or equivalent) for case configuration (case types and review-minute template) and defect attributes. Project settings MUST NOT present these entries after the change. Only users with team role `team.create-by` or `team.admin` (via corresponding menu permissions) SHALL open the management UIs and call write APIs.

#### Scenario: Team admin opens case settings from team options

- **WHEN** a team admin opens team settings
- **THEN** they can navigate to case type management and review template editing for that team

#### Scenario: Project settings no longer link case or defect field config

- **WHEN** a project member opens project settings
- **THEN** there is no card or route entry for case types, review template, or defect field management under project options

#### Scenario: Project admin cannot write team config

- **WHEN** a user who is only a project admin (not team create-by/admin) calls a team case-type or defect-field write API
- **THEN** the request is denied and configuration is unchanged

### Requirement: Team-scoped write APIs

The system SHALL expose authenticated management APIs under `/system/team/{teamId}/` for case types, case review template, and defect fields (including builtin layout updates). Write operations MUST require the corresponding `system:team:case-type:*` or `system:team:defect-field:*` permissions. The server MUST verify the caller may administer that `teamId`.

#### Scenario: Update review template for team

- **WHEN** a team create-by user PUTs a Markdown template to `/system/team/{teamId}/case-review-template`
- **THEN** subsequent reads for any project in that team return the new template content

#### Scenario: Reject write for foreign team

- **WHEN** a user with team admin on team A attempts to modify case types for team B
- **THEN** the operation is rejected

### Requirement: Upgrade migrates from earliest project per team

On platform upgrade, for each team that has at least one project, the system MUST copy case types, review template, and defect field definitions (including builtin field layout) from the team's earliest-created project (`create_time` ascending, then `project_id`) into team-scoped storage. Case rows in other projects of the same team MUST have `case_type` remapped by type name to the surviving team type id when possible; otherwise `case_type` MUST be set null. Teams with no projects MUST receive the same default case-type seed and default template behavior previously applied to new projects.

#### Scenario: Multi-project team uses earliest project as source

- **WHEN** a team has projects P-old and P-new and upgrade runs
- **THEN** team configuration matches P-old's prior project-level configuration for the three areas
- **THEN** cases in P-new whose type name exists on P-old point at the team type id for that name

#### Scenario: Empty team gets defaults

- **WHEN** a team has zero projects at upgrade (or on team create with seed)
- **THEN** the team case-type list includes the default seeded names used previously for new projects
