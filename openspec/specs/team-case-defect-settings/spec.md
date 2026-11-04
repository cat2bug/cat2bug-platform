# team-case-defect-settings Specification

## Purpose

用例类型、评审模板、缺陷属性升为团队级配置；团队设置入口与迁移规则。
## Requirements
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

### Requirement: Defect settings card links to page settings

团队设置中的缺陷属性卡片 MUST 在现有缺陷字段管理入口之外，提供「页面设置」导航，进入团队缺陷页面设置 UI。页面设置入口 MUST 挂在缺陷设置卡片内，MUST NOT 以名为「页面设置」的独立顶层枢纽卡片出现。仅具备团队缺陷字段列表/管理权限的用户 SHALL 看到并进入该入口。枢纽页 MUST 在信息、用例与计划、缺陷、成员之外，允许存在源码 / 交付物 / 文档等业务设置卡片（由团队模块页面设置能力定义）。

#### Scenario: Team admin opens page settings from defect card

- **WHEN** 团队管理员打开团队设置并进入缺陷属性卡片
- **THEN** 可以看到并打开页面设置，且不存在名为「页面设置」的独立顶层卡片

#### Scenario: Hub may include other module setting cards

- **WHEN** 用户打开团队设置枢纽页
- **THEN** 可以同时看到缺陷设置卡与其它业务模块设置卡（如源码、交付物、文档），且「页面设置」仍不是独立顶层卡片标题

### Requirement: Case settings card covers plan and page settings

团队设置中的用例相关卡片 MUST 呈现为「用例与计划设置」（或等价文案），MUST 保留用例类型与评审模板管理入口，并 MUST 提供「页面设置」导航以配置用例与计划的显示开关与两个显示名。

#### Scenario: Open case-plan page settings

- **WHEN** 团队管理员打开用例与计划设置卡
- **THEN** 可以进入页面设置，且仍可导航到用例类型与评审模板

