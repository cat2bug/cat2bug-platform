# git-repo-member-invite Specification

## Purpose

Invite users to a repository ACL via shareable links (3-day TTL), direct add, and from-repo batch invite; reuse `/invite/{token}` for accept.

## Requirements
### Requirement: Repo admin can create a multi-use invite link

具备目标仓 effective manage 的用户 SHALL 能为该仓库创建邀请链接，并指定单一 ACL 角色（`read` / `write` / `admin`）。链接有效期 MUST 固定为自创建时刻起 **3 天**。创建请求 MUST NOT 接受自定义有效期。系统 MUST 将记录以 `scope=REPO` 写入 `sys_member_invite`，并设置 `repository_id` 及所属 `project_id`/`team_id`。

#### Scenario: Create repo invite link

- **WHEN** 具备 effective manage 的用户提交仓库 ID 与 ACL 角色
- **THEN** 系统创建 `scope=REPO` 的邀请记录并返回含 token 的 URL
- **THEN** `expires_at` 等于创建时间加 3 天
- **THEN** `role_ids` 存该 ACL 角色的 JSON 字符串数组（例如 `["write"]`）

#### Scenario: Create denied without manage

- **WHEN** 用户对目标仓无 effective manage
- **THEN** 系统拒绝创建邀请链接

### Requirement: Repo admin can list and revoke active invite links

具备 effective manage 的用户 SHALL 能列出该仓未过期且未撤销的邀请链接，并 SHALL 能吊销指定链接。吊销后该 token MUST NOT 再被接受。重新生成 MUST 先吊销旧链接再创建新链接。

#### Scenario: Revoke active repo invite

- **WHEN** 管理员吊销某 REPO 邀请
- **THEN** 后续对该 token 的 accept 失败并提示已撤销或无效

### Requirement: User accepts repo invite via landing page

系统 SHALL 复用 `/invite/{token}`。未登录用户 MUST 被引导登录或注册并回跳。已登录用户接受有效 REPO 邀请后，系统 MUST 仅向 `sys_git_repository_member` 写入对应 ACL 行，MUST NOT 自动加入项目或团队。

#### Scenario: Unauthenticated user opens repo invite

- **WHEN** 未登录用户访问有效 REPO 邀请落地页
- **THEN** 系统保存回跳地址并引导登录或注册

#### Scenario: Authenticated user accepts repo invite

- **WHEN** 已登录用户接受有效 REPO 邀请
- **THEN** 用户成为该仓 ACL 成员且角色与链接一致
- **THEN** 用户未被写入项目/团队成员表（除非其本已是成员）

#### Scenario: Already ACL member

- **WHEN** 已是该仓 ACL 成员的用户再次接受同一有效链接
- **THEN** 系统提示已是成员且不重复插入冲突行

#### Scenario: Expired or revoked repo invite

- **WHEN** 用户接受已过期或已撤销的 REPO token
- **THEN** 接受失败并给出明确错误

### Requirement: Invite preview shows repository for REPO scope

公开 preview 接口对 `REPO` 邀请 SHALL 返回仓库展示名、邀请人、ACL 角色展示、过期时间与状态，MUST NOT 要求登录。

#### Scenario: Preview valid repo invite

- **WHEN** 请求有效 REPO 邀请的 preview
- **THEN** 返回仓库名称与 `valid` 状态及角色信息

### Requirement: Settings members page offers three invite methods

仓库 settings 成员页 SHALL 提供「添加成员」入口，包含三个方式：链接邀请（含复制与二维码）、直接添加系统用户、从其他仓库邀请。直接添加 MUST 继续使用现有成员添加 API。

#### Scenario: Open invite dialog with three tabs

- **WHEN** 具备 effective manage 的用户打开添加成员
- **THEN** 可见链接邀请、直接添加、通过仓库邀请三个入口

### Requirement: Invite members from another repository

具备目标仓 effective manage 的用户 SHALL 能从源仓批量选择用户并写入目标仓 ACL。源仓 MUST 与目标仓同属一项目、不得为本仓，且调用方对该源仓 MUST 具备 effective manage。可选角色为 `read` / `write` / `admin`。已是目标仓成员的用户 MUST 被跳过而非失败整批。

#### Scenario: Batch invite from eligible source repo

- **WHEN** 管理员选择合法源仓、若干用户与角色并提交
- **THEN** 非成员用户写入目标仓 ACL
- **THEN** 已是成员的用户被跳过并在结果中体现

#### Scenario: Reject ineligible source repo

- **WHEN** 源仓不同项目、为本仓、或调用方对源仓无 manage
- **THEN** 系统拒绝该批量邀请请求

