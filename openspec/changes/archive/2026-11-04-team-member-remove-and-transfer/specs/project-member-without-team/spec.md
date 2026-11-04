## MODIFIED Requirements

### Requirement: Project-only member sees read-only team context

仅拥有项目成员关系、无对应团队 `sys_user_team` 记录的用户（「仅项目挂职」），在进入其挂职项目时 SHALL 能使用团队选择器在「有正式队籍或该队下有项目挂职」的团队之间切换，并正常打开已挂职项目。侧栏 MUST NOT 展示团队管理、团队设置、团队成员管理等团队级入口。当当前选中团队对该用户不是正式队籍时，团队名称可以可切换形式展示，但 MUST NOT 暴露团队写入口。

#### Scenario: Project-only member opens project

- **WHEN** 用户仅有某队下项目的 `sys_user_project` 且无该队 `sys_user_team`
- **THEN** 侧栏显示项目功能菜单
- **THEN** 团队管理与团队选项菜单不可见或不可用

#### Scenario: Project-only member can select affiliated teams

- **WHEN** 用户对团队 A 无正式队籍但对 A 下某项目有成员关系
- **THEN** 团队选择器列表包含团队 A
- **THEN** 用户可选中 A 并进入其已挂职项目进行业务操作

#### Scenario: Unrelated teams stay hidden

- **WHEN** 用户对团队 B 既无 `sys_user_team` 也无 B 下任何 `sys_user_project`
- **THEN** 团队选择器不包含团队 B

## ADDED Requirements

### Requirement: My-team list unions formal membership and project affiliation

`GET /system/team/my` (and any client list backing the team switcher) SHALL return the distinct set of teams where the user either has a `sys_user_team` row **or** has at least one `sys_user_project` on a project belonging to that team.

#### Scenario: Former teammate still sees team

- **WHEN** a user was removed from a team but still has project membership under that team
- **THEN** that team appears in the my-team / switcher list

### Requirement: Project list hides all-projects tab for non-formal members

On the project list page for the current team, when the current user is **not** a formal team member of that team, the UI MUST NOT show the「全部项目」tab and MUST only show「我参与的」(member-only list). Formal team members keep both tabs. The backend MUST still enforce that non-formal members cannot rely on an unfiltered team-wide project browse as a privilege beyond their project seats (member-only listing remains authoritative for them).

#### Scenario: Affiliate sees only participated tab

- **WHEN** a project-only affiliate opens the project list for that team
- **THEN** only the「我参与的」tab is available
- **THEN** the「全部项目」tab is not shown

#### Scenario: Formal member keeps both tabs

- **WHEN** a formal team member opens the project list
- **THEN** both「我参与的」and「全部项目」tabs remain available

### Requirement: Project-only affiliate cannot perform team-scoped create or invite

Users without formal `sys_user_team` for the current team MUST NOT create projects in that team, MUST NOT invite or create team members, and MUST NOT change team-level settings. The client MUST hide or disable those actions; the server MUST reject corresponding APIs with 403 (or equivalent).

#### Scenario: Create project blocked

- **WHEN** a project-only affiliate calls create-project for that team
- **THEN** the request is rejected

#### Scenario: Team invite blocked

- **WHEN** a project-only affiliate calls team invite or team member create for that team
- **THEN** the request is rejected
