# member-invite-link Specification

## Purpose

Provide multi-use invite links for team and project membership (and REPO ACL via extended scope), with fixed TTL, revoke, public preview, and authenticated accept on `/invite/{token}`.

## Requirements

### Requirement: Team admin can create a multi-use invite link

团队管理员（具备团队成员管理权限）SHALL 能为指定团队生成邀请链接，并指定一个或多个团队角色。链接有效期 MUST 固定为自创建时刻起 **1 小时**，创建请求 MUST NOT 接受自定义有效期参数。

#### Scenario: Create team invite link with roles

- **WHEN** 具备权限的管理员提交团队 ID 与团队角色 ID 列表
- **THEN** 系统创建邀请记录并返回含 token 的完整 URL
- **THEN** 记录的 `expires_at` 等于创建时间加 1 小时

#### Scenario: Expiry is not configurable

- **WHEN** 管理员在创建链接 UI 或 API 中尝试设置有效期
- **THEN** 系统不提供该配置项，且忽略任何非法的有效期字段

### Requirement: Project admin can create a multi-use invite link

项目管理员（具备项目成员管理权限）SHALL 能为指定项目生成邀请链接，并指定一个或多个项目角色。链接有效期 MUST 固定为 **1 小时**。项目邀请 MUST NOT 将用户加入团队（`sys_user_team`）。

#### Scenario: Create project invite link

- **WHEN** 具备权限的管理员提交项目 ID 与项目角色 ID 列表
- **THEN** 系统创建 scope 为 PROJECT 的邀请记录并返回 URL
- **THEN** 记录关联 `project_id` 及项目所属 `team_id`（仅上下文）

### Requirement: Invite link can be used by multiple users until expiry

在链接未过期且未被撤销前，多个不同用户 SHALL 能依次使用同一 token 接受邀请并成为成员。

#### Scenario: Second user accepts same token

- **WHEN** 用户 A 已通过 token 成功加入
- **AND** 用户 B 在有效期内使用同一 token 接受邀请
- **THEN** 用户 B 成功加入对应团队或项目

#### Scenario: Link expires

- **WHEN** 当前时间晚于 `expires_at`
- **THEN** 接受操作失败并提示链接已过期

### Requirement: Admin can revoke an invite link

管理员 SHALL 能撤销尚未过期的邀请链接；撤销后该 token MUST NOT 再被接受。

#### Scenario: Revoke active link

- **WHEN** 管理员撤销某邀请记录
- **THEN** 后续接受请求返回链接无效或已撤销

### Requirement: Logged-in user accepts invite via landing page

系统 SHALL 提供 `/invite/{token}` 落地页。未登录用户 MUST 被引导登录；登录后 MUST 能完成接受。已登录用户打开有效链接 MUST 能加入并获得链接上预设的角色。

#### Scenario: Unauthenticated user opens invite

- **WHEN** 未登录用户访问 `/invite/{token}`
- **THEN** 系统保存回跳地址并跳转登录页

#### Scenario: Authenticated user accepts team invite

- **WHEN** 已登录用户接受有效的团队邀请
- **THEN** 用户写入 `sys_user_team` 及对应团队角色
- **THEN** 用户未被重复加入时提示已是团队成员

#### Scenario: Authenticated user accepts project invite

- **WHEN** 已登录用户接受有效的项目邀请
- **THEN** 用户写入 `sys_user_project` 及对应项目角色
- **THEN** 用户 MUST NOT 被加入 `sys_user_team`

### Requirement: Invite preview without authentication

系统 SHALL 提供无需登录的预览接口，用于落地页展示邀请目标名称（团队名或项目名）及是否过期/撤销，MUST NOT 暴露可滥用的敏感信息。

#### Scenario: Preview valid team invite

- **WHEN** 请求有效团队的邀请预览
- **THEN** 返回团队名称与链接状态（有效/过期/撤销）

### Requirement: Preview and accept support REPO scope

成员邀请公开 preview 与 accept 流程 SHALL 识别 `scope=REPO` 的 `sys_member_invite` 记录。对 REPO，accept MUST 将当前用户写入目标仓库 ACL（`sys_git_repository_member`），使用邀请记录中的 ACL 角色，且 MUST NOT 将用户加入团队或项目成员表。TEAM/PROJECT 的 1 小时有效期与既有加入行为 MUST 保持不变。

#### Scenario: Accept REPO invite writes repository ACL only

- **WHEN** 已登录用户接受有效的 `scope=REPO` 邀请
- **THEN** 系统写入对应 `repository_id` 的 ACL 成员行
- **THEN** 系统不因此次接受而新增 `sys_user_team` 或 `sys_user_project` 行

#### Scenario: TEAM and PROJECT invites unchanged

- **WHEN** 用户创建或接受 TEAM 或 PROJECT 邀请
- **THEN** 有效期仍为 1 小时且成员写入路径与既有行为一致
