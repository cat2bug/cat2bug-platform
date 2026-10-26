## MODIFIED Requirements

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

## ADDED Requirements

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
