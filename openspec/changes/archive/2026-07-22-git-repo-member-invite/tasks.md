## 1. Schema and domain

- [x] 1.1 运行或手写 Flyway `V1_1_0_10__git_repo_member_invite.sql`（h2+mysql）：`sys_member_invite.repository_id` + 索引（若序号冲突取下一 n）
- [x] 1.2 `MemberInviteScope.REPO`；`SysMemberInvite.repositoryId`；Mapper/XML 映射与 `selectActiveByRepositoryId` / revoke-by-repo

## 2. Invite service (REPO)

- [x] 2.1 `createRepoInviteLink` / `listActiveRepoInviteLinks` / `revokeRepoInviteLink`（TTL 3 天；`role_ids` 存 ACL JSON）
- [x] 2.2 `preview` / `accept` / `isAlreadyMember` 增加 REPO 分支（写 ACL；VO 含 repository 信息与 redirect）
- [x] 2.3 单元测试：3 天过期、吊销、accept、already-member、过期拒绝

## 3. Repo management APIs

- [x] 3.1 Controller：`GET/POST .../invites`、`POST .../invites/{id}/revoke`（effective manage）
- [x] 3.2 `invite-sources`、源仓 members、`POST .../members/from-repo`（同项目 + 源仓 manage；跳过已有成员）
- [x] 3.3 from-repo / sources 相关单元测试

## 4. Frontend

- [x] 4.1 `gitRepo.js` 增加 invites / sources / from-repo API helpers
- [x] 4.2 settings `members` 三 Tab：链接（复制+二维码+重生）、直接添加、从仓邀请 + i18n
- [x] 4.3 `/invite/:token` 支持 REPO 展示与成功跳转仓页

## 5. Verify

- [ ] 5.1 重启后端后手工验收三 Tab 与链接登录回跳；勾选本 tasks
