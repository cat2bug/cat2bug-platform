## Context

一期 `git-repo-settings` 已上线壳内管理与侧栏「仓库成员」灰显。权限仍由 `GitRepositoryAccessService` 仅按**项目角色**判定（`canPull/canPush/canManage(userId, projectId)`）。产品已决议仓级 ACL（见一期 design D8）。约束：JGit/现有 Smart HTTP；Flyway 双端（h2+mysql）；版本线 `cat2bug.version` = 1.1.0（下一序号用 `./scripts/new-flyway-migration.sh`）。

## Goals / Non-Goals

**Goals:**

- 仓级 ACL 表 + CRUD API + settings 成员页
- effectivePerm：有 ACL 行覆盖；无行回落项目角色；可邀请非项目成员
- 创建/导入仓库时写入创建者 `admin`
- REST、Smart HTTP、壳「管理」Tab / settings 守卫共用 effective manage
- 禁止移除最后一位仓 `admin`

**Non-Goals:**

- 保护分支/标签、WebHook、部署公钥、团队角色绕过
- 非项目成员的「我参与的仓库」全局列表（本期可直接 URL/clone 访问）
- 回填历史仓库的默认 admin 行以外的复杂迁移策略（见 Migration：可选批量补创建者）

## Decisions

### D1. 表结构

- **选择**：`sys_git_repository_member`（或等价名）  
  `(repository_id, user_id)` PK/UK，`role` ∈ {`read`,`write`,`admin`}，审计字段可选。  
- **替代**：JSON 列挂在 `sys_git_repository` → 难查难约束。  
- **Flyway**：`./scripts/new-flyway-migration.sh git_repo_members` 生成 h2+mysql。

### D2. AccessService 签名改为按仓库

- **选择**：
  ```
  canPull(userId, repositoryId)
  canPush(userId, repositoryId)
  canManage(userId, repositoryId)
  ```
  实现内：查 ACL 行 → 映射；否则 load repo.projectId → 现有项目角色矩阵。  
- **替代**：保留 `(userId, projectId)` 另加 overload → 调用方易漏传 repoId，ACL 永不生效。  
- **理由**：强制所有路径带仓库上下文。

角色映射：

| 仓角色 | pull | push | manage/force-push |
|--------|------|------|-------------------|
| read | ✓ | ✗ | ✗ |
| write | ✓ | ✓ | ✗ |
| admin | ✓ | ✓ | ✓ |

项目回落（无 ACL 行）：develop/tester → write；create-by/admin → admin（与今日一致）。

### D3. 调用点迁移

- Smart HTTP / browse / PR / release / repo CRUD：凡 `can*(user, projectId)` 改为 `can*(user, repositoryId)`。  
- 列表类「项目内是否可见仓库」：项目成员仍可见项目下仓库；**仅有仓 ACL、非项目成员**本期不进项目列表（Non-Goal），但 clone/深链按 ACL 放行。

### D4. 成员 API

- `GET  /system/git/repo/{id}/members` — 列表（需 effective manage 或 pull？**选择：manage 才可看完整 ACL 列表**；普通成员不需要）  
- `POST /system/git/repo/{id}/members` — body: userId + role（可邀请非项目成员；user 须存在）  
- `PUT  /system/git/repo/{id}/members/{userId}` — 改角色  
- `DELETE .../members/{userId}` — 移除；若该用户是最后一位 admin → 400  
- 可选：`GET /system/git/repo/{id}/my-access` 返回当前用户 effective 角色，供前端 Tab/守卫

权限：写操作需 `canManage(user, repositoryId)`。

### D5. 创建时默认 admin

- **选择**：`insertSysGitRepository` / import 成功路径在同一事务写入创建者 `admin` 行。  
- 历史仓：迁移脚本或启动时一次性为「无任何 admin 行」的仓补 `create_by` 为 admin（推荐在 Flyway 后跟数据补丁或应用层 ensure）。

### D6. UI

- 路由：`GitRepoSettingsMembers` → `.../settings/members`  
- 侧栏启用「仓库成员管理」；去掉灰显  
- 页：成员表（用户、角色下拉、移除）+ 添加（用户搜索/选择 + 角色）  
- 壳 Tab / settings `beforeEnter`：改用后端 effective manage（前端 `checkPermi` 不足时以 `my-access` 或接口 403 为准；**推荐** settings 守卫仍要 `system:git:repo:manage` **或** 新权限字，但仓 admin 非项目 admin 可能无菜单权限字——**选择**：后端 403 为准；前端 Tab 显示调用 `my-access`，`role===admin` 则显示管理 Tab，避免仅靠权限字）

### D7. 与权限字关系

- 现网 `@PreAuthorize` / `v-hasPermi` 挂项目角色权限字。仓 admin 若不是项目 admin，可能无 `system:git:repo:manage`。  
- **选择**：成员与危险操作 API 以 `GitRepositoryAccessService.canManage(repoId)` 为权威；控制器在权限字之外（或放宽）再调 service 校验。前端管理 Tab 以 `my-access` 为准，不单靠 `v-hasPermi`。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 漏改某个 can*(projectId) 调用 | 编译期改签名；全仓 grep；单测矩阵 |
| 历史仓无 admin 行 | 补丁：无 admin 则 create_by → admin |
| 仓 admin 无权限字看不到 Tab | my-access 驱动 Tab；API 以 service 为准 |
| 最后一位 admin 被删 | DELETE/降权校验 |
| 项目 admin 被 ACL 降权后无法自救 | 另一无降权行的项目 admin 或仓 admin 改回 |

## Migration Plan

1. Flyway 建表（h2+mysql）  
2. 数据补丁：每个仓库至少一名 admin（优先 create_by）  
3. 发布后端 AccessService + API；再开前端成员页  
4. 回滚：关成员 API/UI；AccessService 可临时忽略 ACL 表（不推荐）；表可保留

## Open Questions

- 无阻塞项。用户搜索组件复用现有系统用户选择器即可。
