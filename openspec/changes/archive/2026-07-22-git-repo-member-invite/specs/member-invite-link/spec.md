## ADDED Requirements

### Requirement: Preview and accept support REPO scope

成员邀请公开 preview 与 accept 流程 SHALL 识别 `scope=REPO` 的 `sys_member_invite` 记录。对 REPO，accept MUST 将当前用户写入目标仓库 ACL（`sys_git_repository_member`），使用邀请记录中的 ACL 角色，且 MUST NOT 将用户加入团队或项目成员表。TEAM/PROJECT 的 1 小时有效期与既有加入行为 MUST 保持不变。

#### Scenario: Accept REPO invite writes repository ACL only

- **WHEN** 已登录用户接受有效的 `scope=REPO` 邀请
- **THEN** 系统写入对应 `repository_id` 的 ACL 成员行
- **THEN** 系统不因此次接受而新增 `sys_user_team` 或 `sys_user_project` 行

#### Scenario: TEAM and PROJECT invites unchanged

- **WHEN** 用户创建或接受 TEAM 或 PROJECT 邀请
- **THEN** 有效期仍为 1 小时且成员写入路径与既有行为一致
