## Context

仓级 ACL（`sys_git_repository_member`）与 settings 成员 CRUD / `my-access` 已存在；团队/项目已有通用邀请表 `sys_member_invite` 与公开落地页 `/invite/:token`（1 小时 TTL）。本 change 在仓 settings 上补齐三种邀请入口，并扩展同一邀请表以支持 `REPO`。

详细产品决策见 `readme/core/2026-07-22-git-repo-member-invite-design.md` 与实现计划 `readme/core/2026-07-22-git-repo-member-invite-plan.md`。

## Goals / Non-Goals

**Goals:**

- 仓 effective manage 可创建 3 天有效、可吊销的多用户邀请链接（角色 read/write/admin）
- 未登录用户经登录/注册后接受链接，仅写入仓 ACL
- settings 三 Tab：链接（含二维码）、直接添加、从同项目可 manage 的其他仓批量邀请
- 扩展现有 preview/accept，不新建并行邀请表

**Non-Goals:**

- 邀请审核、可配置有效期、AI 队友、协作者上限
- 改造团队/项目邀请 UI 或将其 TTL 改为 3 天

## Decisions

1. **扩展 `sys_member_invite`，而非新建表**  
   - 备选：独立 `sys_git_repo_invite` → 拒绝，避免双栈 preview/accept。  
   - 新增 `repository_id`；`scope=REPO`；创建时回填 `team_id`/`project_id`。

2. **`role_ids` 对 REPO 存 ACL 字符串 JSON（如 `["write"]`）**  
   - 备选：新列 `repo_role` → 可后续再加；本期按 scope 分支解析，避免把字符串当 Long。

3. **TTL 分 scope**  
   - REPO：固定 3×24h；TEAM/PROJECT：保持 3600s。

4. **管理 API 挂在 `/system/git/repo/{id}/…`**  
   - 与 members API 同控制器；公开流仍走 `/invite/{token}/preview|accept`。

5. **from-repo 源仓约束**  
   - 同 `project_id`、非本仓、调用方对源仓与目标仓均 effective manage。

6. **accept 成功跳转**  
   - `MemberInviteAcceptResultVo` 增加 `repositoryId` 与/或 `redirectPath`，落地页优先跳仓代码页。

## Risks / Trade-offs

- [Risk] `role_ids` 双语义解析错误 → 所有 REPO 路径按 scope 分支，单测覆盖  
- [Risk] 热加载不注册新 Mapping → 文档要求完整重启后端  
- [Risk] 无审核时链接泄露 → 可吊销 + 3 天过期；后续可加审核  

## Migration Plan

1. Flyway 增加 `repository_id`（h2+mysql，版本线 1.1.0 下一序号）  
2. 部署后旧 TEAM/PROJECT 行不受影响  
3. 回滚：停用 REPO API/UI；列可保留  

## Open Questions

无（产品决策已确认）。
