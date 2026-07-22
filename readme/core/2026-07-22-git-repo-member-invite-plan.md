# Git Repo Member Invite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 仓库 settings 成员页支持链接邀请（3 天 + 二维码）、直接添加、从同项目其他仓批量邀请；扩展现有 `sys_member_invite` 与 `/invite/:token`，接受后写入仓 ACL。

**Architecture:** 复用 `sys_member_invite`（新增 `repository_id` + scope `REPO`）；仓管理 API 挂在 `/system/git/repo/{id}/…`；公开 preview/accept 扩展 `SysMemberInviteServiceImpl`；前端成员页三 Tab 对话框 + 落地页展示仓库。团队/项目有效期仍为 1h，仓为 3d。

**Tech Stack:** Spring Boot 3.3 / Java 17、MyBatis、Flyway（h2+mysql）、Vue 2.7、Element Plus、已有 `qrcode` 依赖。

**Design spec:** `readme/core/2026-07-22-git-repo-member-invite-design.md`

---

## File map

| 文件 | 职责 |
|------|------|
| `db/migration/{h2,mysql}/V1_1_0_10__git_repo_member_invite.sql` | `repository_id` 列 + 索引 |
| `MemberInviteScope.java` | 增加 `REPO` |
| `SysMemberInvite.java` + Mapper XML | `repositoryId` |
| `MemberInvitePreviewVo` / `AcceptResultVo` | `repositoryName` / `repositoryId` 等 |
| `ISysMemberInviteService` + `SysMemberInviteServiceImpl` | create/list/revoke REPO；preview/accept 分支 |
| `SysGitRepositoryController`（或独立 invite 段） | 仓 invites / from-repo / invite-sources |
| `ISysGitRepositoryMemberService` | `insertMembersFromRepo`（可选放在 member service） |
| `gitRepo.js` / `memberInvite.js` | 前端 API |
| `settings/members.vue` + 子组件 | 三 Tab 邀请 UI |
| `views/invite/index.vue` | REPO 展示与跳转 |
| 单元测试 | InviteService REPO + Member from-repo |

---

### Task 1: Flyway — `repository_id`

**Files:**
- Create: `cat2bug-platform-admin/src/main/resources/db/migration/h2/V1_1_0_10__git_repo_member_invite.sql`
- Create: `cat2bug-platform-admin/src/main/resources/db/migration/mysql/V1_1_0_10__git_repo_member_invite.sql`

（若仓库已有更高序号，先 `ls …/h2/V1_1_0_*.sql` 取下一 `n`，或跑 `./scripts/new-flyway-migration.sh git_repo_member_invite`。）

- [ ] **Step 1: 写 h2 脚本**

```sql
ALTER TABLE sys_member_invite ADD COLUMN IF NOT EXISTS repository_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_sys_member_invite_repository ON sys_member_invite (repository_id);
```

- [ ] **Step 2: 写 mysql 脚本**（无 `IF NOT EXISTS` 时用可重复语义或纯 `ADD COLUMN`）

```sql
ALTER TABLE sys_member_invite ADD COLUMN repository_id BIGINT NULL COMMENT '仓库邀请目标' AFTER project_id;
CREATE INDEX idx_sys_member_invite_repository ON sys_member_invite (repository_id);
```

- [ ] **Step 3: Commit**

```bash
git add cat2bug-platform-admin/src/main/resources/db/migration/h2/V1_1_0_10__git_repo_member_invite.sql \
        cat2bug-platform-admin/src/main/resources/db/migration/mysql/V1_1_0_10__git_repo_member_invite.sql
git commit -m "$(cat <<'EOF'
feat(db): add repository_id to sys_member_invite for repo ACL invites

EOF
)"
```

---

### Task 2: Domain — scope + entity + mapper

**Files:**
- Modify: `cat2bug-platform-system/src/main/java/com/cat2bug/system/domain/type/MemberInviteScope.java`
- Modify: `cat2bug-platform-system/src/main/java/com/cat2bug/system/domain/SysMemberInvite.java`
- Modify: `cat2bug-platform-system/src/main/resources/mapper/system/SysMemberInviteMapper.xml`
- Modify: `cat2bug-platform-system/src/main/java/com/cat2bug/system/mapper/SysMemberInviteMapper.java`

- [ ] **Step 1: 增加 scope 常量**

```java
public static final String REPO = "REPO";
```

- [ ] **Step 2: Entity 增加 `private Long repositoryId;`**

- [ ] **Step 3: Mapper**  
  - `insert` / `selectByToken` / resultMap 映射 `repository_id`  
  - 新增：`selectActiveByRepositoryId(repositoryId, now)`  
  - 扩展 revoke：支持按 `repositoryId` 校验归属（`revokeSysMemberInvite(..., repositoryId)` 或新方法）

- [ ] **Step 4: Commit**

```bash
git commit -m "feat: map repository_id for member invite REPO scope"
```

---

### Task 3: Invite service — create / list / revoke REPO（TDD）

**Files:**
- Modify: `ISysMemberInviteService.java`
- Modify: `SysMemberInviteServiceImpl.java`
- Modify: `SysMemberInviteServiceTest.java`（或新建 `SysMemberInviteRepoScopeTest.java`）
- Modify: VOs as needed (`CreateMemberInviteVo` 可复用；仓侧用 `role` 字符串而非 `roleIds`)

**约定：**
- `role_ids` 列存 JSON：`["write"]`（ACL 字面量，**不是** Long）
- TTL：`REPO_INVITE_TTL_SECONDS = 3 * 24 * 3600`；TEAM/PROJECT 仍用 `3600`
- 创建时从仓库查出 `projectId`/`teamId` 一并写入

- [ ] **Step 1: 写失败测试**（Mockito）

```java
@Test
void createRepoInvite_setsThreeDayExpiryAndAclRoleJson() {
    // mock repo + mapper insert
    // assert expiresAt ≈ now+3d
    // assert scope REPO, roleIds equals ["write"]
}

@Test
void acceptRepoInvite_insertsAclMember() { ... }

@Test
void acceptRepoInvite_whenAlreadyMember_setsFlag() { ... }

@Test
void acceptRepoInvite_whenExpired_throws() { ... }
```

- [ ] **Step 2: 跑测试确认失败**

```bash
mvn -pl cat2bug-platform-system -am test -Dtest=SysMemberInviteServiceTest,SysMemberInviteRepoScopeTest
```

Expected: 编译失败或断言失败（方法尚不存在）。

- [ ] **Step 3: 实现接口方法**

```java
MemberInviteLinkVo createRepoInviteLink(Long repositoryId, String aclRole, Long createdBy, String inviteBaseUrl);
List<MemberInviteLinkVo> listActiveRepoInviteLinks(Long repositoryId, String inviteBaseUrl);
int revokeRepoInviteLink(Long repositoryId, Long inviteId);
```

在 `preview` / `isAlreadyMember` / `accept` 增加 `REPO` 分支：
- preview：`repositoryName`；`roleNames` 用 i18n key 或直接 `read`/`write`/`admin` 展示名（勿走 `sys_role`）
- accept：调用 `sysGitRepositoryMemberService.insertMember(repositoryId, userId, aclRole)`；捕获「已是成员」→ `alreadyMember=true`
- `AcceptResultVo` 增加 `repositoryId`（成功跳转用）

- [ ] **Step 4: 跑测试通过**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: REPO scope create/preview/accept for member invites"
```

---

### Task 4: Controller — 仓邀请与从仓导入 API

**Files:**
- Modify: `SysGitRepositoryController.java`
- Possibly: `SysGitRepositoryMemberServiceImpl.java`（批量 from-repo）
- Test: controller 或 service 单测

**Endpoints（均需登录；写操作校验 effective manage）：**

```text
GET    /system/git/repo/{repositoryId}/invites
POST   /system/git/repo/{repositoryId}/invites          body: { "role": "write" }
POST   /system/git/repo/{repositoryId}/invites/{inviteId}/revoke
GET    /system/git/repo/{repositoryId}/invite-sources
GET    /system/git/repo/{repositoryId}/invite-sources/{sourceRepositoryId}/members
POST   /system/git/repo/{repositoryId}/members/from-repo body: { sourceRepositoryId, userIds[], role }
```

- [ ] **Step 1: from-repo 服务逻辑测试**

```java
@Test
void fromRepo_rejectsWhenSourceNotSameProject() { ... }

@Test
void fromRepo_rejectsWhenCallerCannotManageSource() { ... }

@Test
void fromRepo_skipsExistingMembersAndInsertsOthers() { ... }
```

- [ ] **Step 2: 实现**  
  - sources：同 `projectId`、排除本仓、`canManage(userId, sourceId)`  
  - from-repo：对每个 `userId` 调 `insertMember`；重复跳过；返回 `{ added, skipped }`

- [ ] **Step 3: Controller 挂载 + `@PreAuthorize` 与现有 members 一致（粗粒度权限字 + 服务内 effective manage）**

- [ ] **Step 4: 跑相关测试**

```bash
mvn -pl cat2bug-platform-system,cat2bug-platform-admin -am test \
  -Dtest=SysMemberInvite*,SysGitRepositoryMember*,*GitRepository*Access*
```

- [ ] **Step 5: Commit**

```bash
git commit -m "feat: git repo invite-link and from-repo member APIs"
```

---

### Task 5: Frontend API

**Files:**
- Modify: `cat2bug-platform-ui/src/api/system/gitRepo.js`

- [ ] **Step 1: 增加方法**

```js
export function listGitRepoInvites(repositoryId) {
  return request({ url: `/system/git/repo/${repositoryId}/invites`, method: 'get' })
}
export function createGitRepoInvite(repositoryId, data) {
  return request({ url: `/system/git/repo/${repositoryId}/invites`, method: 'post', data })
}
export function revokeGitRepoInvite(repositoryId, inviteId) {
  return request({
    url: `/system/git/repo/${repositoryId}/invites/${inviteId}/revoke`,
    method: 'post'
  })
}
export function listGitRepoInviteSources(repositoryId) {
  return request({ url: `/system/git/repo/${repositoryId}/invite-sources`, method: 'get' })
}
export function listGitRepoInviteSourceMembers(repositoryId, sourceRepositoryId) {
  return request({
    url: `/system/git/repo/${repositoryId}/invite-sources/${sourceRepositoryId}/members`,
    method: 'get'
  })
}
export function addGitRepoMembersFromRepo(repositoryId, data) {
  return request({
    url: `/system/git/repo/${repositoryId}/members/from-repo`,
    method: 'post',
    data
  })
}
```

- [ ] **Step 2: Commit**

```bash
git commit -m "feat(ui): API helpers for git repo member invites"
```

---

### Task 6: Members UI — 三 Tab 邀请

**Files:**
- Modify: `cat2bug-platform-ui/src/views/system/git/repo/settings/members.vue`
- Create (建议拆分):  
  - `.../settings/components/InviteMemberDialog.vue`  
  - 或同目录 `InviteByLinkTab.vue` / `InviteDirectTab.vue` / `InviteFromRepoTab.vue`
- Modify: `i18n-zh-CN.json` / `i18n-en-US.json`（`git.repo.settings.members.invite.*`）
- Reuse: `buildInviteUrl` from `memberInvite.js`；`qrcode` 包生成 data URL

- [ ] **Step 1: 列表上方「添加成员」打开 Dialog，`el-tabs`：**
  - **链接邀请**：角色 select → 加载/创建 invite → 只读 URL、复制、`<img :src="qrDataUrl">`、文案「3 天有效」；按钮「重新生成」= revoke + create  
  - **直接添加**：把现有表单挪进此 Tab  
  - **通过仓库邀请**：源仓 select → 成员多选 table → 角色 → 提交 `from-repo`

- [ ] **Step 2: 手工点一遍三 Tab（需后端已重启）**

- [ ] **Step 3: Commit**

```bash
git commit -m "feat(ui): three-tab invite dialog on git repo members settings"
```

---

### Task 7: 落地页 `/invite/:token` 支持 REPO

**Files:**
- Modify: `cat2bug-platform-ui/src/views/invite/index.vue`
- Modify: i18n `invite.*`（仓库名、角色文案、成功跳转）

- [ ] **Step 1: preview 含 `scope==='REPO'` 时展示 `repositoryName`（VO 字段名与后端对齐）**

- [ ] **Step 2: accept 成功后**  
  - 若有 `repositoryId` + 路由上下文：跳转 `/{teamKey}/{projectKey}/code/{slug}`（accept 结果可返回 `teamKey/projectKey/slug`，或前端再查仓详情）  
  - 最小实现：accept VO 增加 `redirectPath` 由后端拼好，避免前端拼路由失败

建议后端 `MemberInviteAcceptResultVo` 增加：

```java
private String redirectPath; // e.g. /{teamKey}/{projectKey}/code/{slug}
```

- [ ] **Step 3: 未登录路径沿用现有 redirect；确认注册后回跳仍有效**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(ui): accept git repo invites on /invite landing page"
```

---

### Task 8: OpenSpec 同步 + 手工验收

**Files:**
- Create: `openspec/changes/git-repo-member-invite/`（`proposal.md` / `design.md` / `tasks.md` / specs），内容对齐本 plan 与 design 文档  
- 或实现完成后再 `openspec` archive

- [ ] **Step 1: 建 change 并勾选 tasks**

- [ ] **Step 4.4 类手工清单：**
  1. 重启后端，确认 `POST/GET .../invites` 非 404  
  2. 链接邀请：复制 → 无痕/退出登录打开 → 登录 → 加入 → ACL 出现  
  3. 过期/吊销链接失败提示  
  4. 从仓邀请：仅列出可 manage 的同项目仓；批量后成员表更新  
  5. 直接添加仍可用  

- [ ] **Step 2: 勾选 `openspec/changes/git-repo-members/tasks.md` 的 4.4（若仍开放）及本 change tasks**

---

## Spec coverage check

| 设计项 | Task |
|--------|------|
| 扩展 `sys_member_invite.repository_id` | 1–2 |
| scope REPO + 3 天 TTL | 3 |
| preview/accept 写 ACL | 3、7 |
| 三 Tab UI + 二维码 | 6 |
| from-repo + sources | 4–6 |
| 无审核 | 全任务均不实现审核 |
| 团队/项目 UI 不动 | 仅扩展表与 accept 分支 |

## Out of scope（勿做）

- 审核流、可配置有效期、AI 队友、协作者上限、改造团队/项目邀请 TTL
