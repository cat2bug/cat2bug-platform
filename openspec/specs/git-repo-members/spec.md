# git-repo-members Specification

## Purpose

Persist per-repository ACL (`read`/`write`/`admin`), compute effective permission (ACL overrides project roles), and expose members settings including invite-by-link and invite-from-repo.

## Requirements
### Requirement: Repository ACL stores per-user roles

The system SHALL persist zero or more ACL rows per repository, each binding a system user to exactly one of `read`, `write`, or `admin` for that repository. A user SHALL have at most one ACL row per repository.

#### Scenario: Add member with write role

- **WHEN** a user with effective repository manage permission adds another existing system user with role `write`
- **THEN** the system stores an ACL row for that user and repository with role `write`

#### Scenario: Duplicate member rejected

- **WHEN** a manage user attempts to add a user who already has an ACL row on that repository
- **THEN** the system rejects the request without creating a second row

### Requirement: Effective permission prefers ACL over project roles

The system SHALL compute repository access as follows: if the user has an ACL row for the repository, only that row’s role applies; otherwise if the user holds a project role that grants git access, the existing project-role matrix applies; otherwise access is denied (subject to existing public-visibility read rules if any). An ACL role of `read` MUST deny push, force-push, settings changes, and ACL administration even if the user is a project admin.

#### Scenario: Project admin demoted by ACL read

- **WHEN** a user holds `project.admin` on the project and an ACL row of `read` on the repository
- **THEN** the user can pull but MUST NOT push, force-push, delete the repository, or modify ACL

#### Scenario: Non-project member with ACL write

- **WHEN** a system user is not a project member but has an ACL row of `write` on the repository
- **THEN** the user can pull and push that repository according to write permissions

#### Scenario: Project develop without ACL row

- **WHEN** a user holds only `project.develop` and has no ACL row on the repository
- **THEN** the user can pull and push but MUST NOT manage the repository

### Requirement: Creator becomes initial repository admin

The system SHALL, when creating or successfully importing a repository, insert an ACL row granting `admin` to the creating user in the same success path as metadata persistence.

#### Scenario: Empty create seeds admin

- **WHEN** a manage user successfully creates an empty repository
- **THEN** an ACL row exists for the creator with role `admin`

#### Scenario: Import seeds admin

- **WHEN** a manage user successfully imports a repository
- **THEN** an ACL row exists for the importer with role `admin`

### Requirement: Last repository admin cannot be removed or demoted away

The system SHALL reject removing an ACL row or changing its role away from `admin` when that would leave the repository with zero `admin` ACL rows.

#### Scenario: Delete last admin denied

- **WHEN** a manage user attempts to delete the only `admin` ACL row for a repository
- **THEN** the system rejects the operation and the row remains

### Requirement: Members settings page

The system SHALL provide an enabled repository settings page at `.../code/{slug}/settings/members` where effective manage users can list, add, update roles for, and remove ACL members. The settings sidebar Members item MUST be navigable (not a disabled placeholder).

#### Scenario: Manage user opens members page

- **WHEN** a user with effective manage permission opens `.../settings/members`
- **THEN** the members list is shown and add/edit/remove actions are available

#### Scenario: Non-manage denied members page

- **WHEN** a user without effective manage permission opens the members settings URL
- **THEN** the system denies access or redirects away without applying ACL changes

### Requirement: Members settings supports invite-by-link and invite-from-repo

仓库成员 settings 页在保留直接添加与成员列表的同时，SHALL 支持通过邀请链接与从其他仓库批量添加成员。相关写操作 MUST 要求目标仓 effective manage。从其他仓库导入时 MUST 遵守同项目与源仓 manage 约束。

#### Scenario: Direct add still works

- **WHEN** 管理员在成员页直接搜索用户并指定角色提交
- **THEN** 系统按既有 members API 写入 ACL

#### Scenario: From-repo API available to managers

- **WHEN** 具备 effective manage 的用户调用从仓导入成员接口并提供合法参数
- **THEN** 系统批量写入 ACL 并跳过已存在成员

