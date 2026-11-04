## Requirements

### Requirement: Project member candidate pool includes all registered users

添加项目成员时，候选用户列表 SHALL 从全局已注册用户（`sys_user`）中选取，排除已在目标项目中的用户。候选池 MUST NOT 要求用户先为同团队成员。当调用方提供非空搜索关键词时，系统 SHALL 按昵称（`nick_name`）、手机号（`phonenumber`）、邮箱（`email`）任一字段模糊匹配；空关键词 MUST NOT 返回全库候选刷列表。

#### Scenario: Invite global user to project via search

- **WHEN** 项目管理员打开添加成员并搜索昵称
- **THEN** 可搜到未加入该项目且未加入该团队的已注册用户

#### Scenario: Search by phone number or email

- **WHEN** 项目管理员在添加成员搜索框输入已注册用户的手机号或邮箱片段
- **THEN** 该用户出现在候选列表中（若尚未加入本项目）

#### Scenario: User already in project excluded

- **WHEN** 某用户已是项目成员
- **THEN** 该用户不出现在项目 not-member 列表中

#### Scenario: Empty search yields no candidates

- **WHEN** 搜索关键词为空或仅空白
- **THEN** 候选列表为空，不返回未过滤的全库用户

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

### Requirement: Backend enforces team boundary for non-team members

对无团队 membership 的用户，后端 MUST 拒绝团队级写操作及团队设置类 API（返回 403 或等价错误），即使用户直链访问 URL。

#### Scenario: Direct API access to team settings

- **WHEN** 仅项目成员的用户请求团队设置或团队成员变更 API
- **THEN** 请求被拒绝

### Requirement: Project invite does not grant team membership

通过项目邀请链接或项目添加成员加入的用户，若此前不是团队成员，加入后 MUST 仍无 `sys_user_team` 记录，仅拥有项目权限。

#### Scenario: Accept project invite as outsider

- **WHEN** 非团队成员接受项目邀请链接
- **THEN** 用户成为项目成员且 `teamMember` 为 false
- **THEN** 用户可访问该项目业务功能

### Requirement: Add-member dropdown shows contact on nameplate

项目「搜索添加」成员下拉 SHALL 使用与缺陷选人一致的成员铭牌展示（`MemberNameplate`）：主行显示昵称（及头像），次行显示手机号与邮箱。组件 MUST 支持多选已注册用户；MUST NOT 允许将未匹配到已有用户的自由文本作为成员提交。

#### Scenario: Dropdown shows phone and email under name

- **WHEN** 搜索返回候选用户且该用户有手机或邮箱
- **THEN** 下拉选项名称下方展示对应手机号与邮箱

#### Scenario: Multi-select registered users only

- **WHEN** 管理员选中多名候选用户并提交
- **THEN** 请求携带多个已存在的用户 ID
- **THEN** 未出现在候选中的纯文本输入不会成为成员

### Requirement: Direct project add persists roles without team membership

通过项目「搜索添加」直接加入的用户，系统 SHALL 写入项目成员关系及所选项目角色；若该用户此前不是团队成员，加入后 MUST 仍无 `sys_user_team` 记录。提交 MUST 包含至少一个角色；`memberIds` 无论以单个数值或数组形式传递，后端 MUST 能正确解析。

#### Scenario: Direct add outsider with roles

- **WHEN** 管理员将非团队的已注册用户加入项目并选择一个或多个项目角色
- **THEN** 用户成为项目成员且拥有所选角色
- **THEN** 用户无对应 `sys_user_team` 记录

#### Scenario: Direct add rejects missing roles

- **WHEN** 管理员提交成员 ID 列表但未选择任何角色
- **THEN** 添加失败并提示角色不能为空

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
