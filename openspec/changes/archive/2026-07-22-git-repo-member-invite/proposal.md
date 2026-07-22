## Why

仓库 settings「成员」目前只能搜索用户直接加 ACL，缺少常见托管平台的「链接邀请 / 从其他仓拉人」能力。仓级 ACL（`git-repo-members`）已落地，需要在同一权限模型上补齐邀请体验，并复用已有团队/项目邀请链路。

## What Changes

- 扩展 `sys_member_invite`：新增 `repository_id`，`scope` 增加 `REPO`
- 仓管理员可创建/查看/吊销仓库邀请链接（固定 **3 天**有效；无审核）
- 复用 `/invite/:token` 落地页：未登录先登录/注册再接受；接受后仅写入 `sys_git_repository_member`
- settings 成员页「添加成员」三 Tab：链接邀请（含二维码）、直接添加、通过仓库邀请
- 从同项目、调用方有 effective manage 的其他仓批量导入成员（角色 `read`/`write`/`admin`）
- 团队/项目邀请行为与 1 小时 TTL **不变**；本期不改其 UI

## Capabilities

### New Capabilities

- `git-repo-member-invite`: 仓库邀请链接、从仓批量邀请、settings 三 Tab UI、落地页 REPO 分支

### Modified Capabilities

- `member-invite-link`: preview/accept 支持 `REPO` scope；仓 TTL 3 天与团队/项目 1 小时并存
- `git-repo-members`: 成员页从「仅直接添加」扩展为三种邀请入口（含 from-repo API）

## Impact

- 后端：Flyway、`SysMemberInvite*`、`SysGitRepositoryController`、仓成员服务
- 前端：`settings/members`、`/invite/:token`、`gitRepo.js`、i18n；依赖已有 `qrcode`
- 无新第三方依赖；不引入审核流
