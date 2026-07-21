## ADDED Requirements

### Requirement: Project member candidate pool includes all registered users

添加项目成员时，候选用户列表 SHALL 从全局已注册用户（`sys_user`）中选取，排除已在目标项目中的用户。候选池 MUST NOT 要求用户先为同团队成员。

#### Scenario: Invite global user to project via search

- **WHEN** 项目管理员打开添加成员并搜索昵称
- **THEN** 可搜到未加入该项目且未加入该团队的已注册用户

#### Scenario: User already in project excluded

- **WHEN** 某用户已是项目成员
- **THEN** 该用户不出现在项目 not-member 列表中

### Requirement: Project-only member sees read-only team context

仅拥有项目成员关系、无 `sys_user_team` 记录的用户，在进入该项目时 SHALL 在界面左上角看到项目所属团队名称作为只读上下文。该用户 MUST NOT 看到团队管理、团队设置、团队成员管理等团队级入口。

#### Scenario: Project-only member opens project

- **WHEN** 用户仅有 `sys_user_project` 记录且无对应 `sys_user_team`
- **THEN** 侧栏显示项目功能菜单
- **THEN** 团队名称以只读标签展示
- **THEN** 团队管理与团队选项菜单不可见或不可用

#### Scenario: Project-only member cannot switch teams

- **WHEN** 用户无团队成员身份
- **THEN** 团队切换器不可用于切换到其他团队

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
