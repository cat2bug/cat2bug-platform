## Context

仓库壳 `GitRepoShell` 现有 Tab：代码 / Pull Request / 发行版。元数据编辑与删除仅在项目仓库列表对话框中，进入浏览后无法就地管理。权限仍由 `GitRepositoryAccessService` 按**项目角色**判定（manage = `project.create-by` | `project.admin`）。产品已决议后续引入仓级 ACL，但本 change 只做管理壳与基本信息/删除，侧栏对「仓库成员」灰显占位。

SPA 路径惯例为 `/{teamKey}/{projectKey}/code/{slug}/…`（产品口述的 `/code/{slug}/settings/...` 指该段后缀）。

## Goals / Non-Goals

**Goals:**

- 壳内「管理」Tab + 嵌套 settings 布局（侧栏 + 内容）
- 路由：`.../code/{slug}/settings` → 重定向 general；`.../settings/general`、`.../settings/danger`（或 delete）
- 基本信息、删除复用现有 update/delete API 与字段
- 列表「设置」跳进壳内 settings
- 侧栏「仓库成员管理」灰显；ACL 决议写入本文供后续 change

**Non-Goals:**

- 仓级 ACL 表、API、Smart HTTP 判定改写（后续 `git-repo-members`）
- 保护分支/标签、WebHook、镜像、GC、转移、清空、部署公钥、挂件等
- 新增 Issues/Wiki「功能设置」

## Decisions

### D1. Settings 作为独立壳内页面，而非 browse 内 `?tab=settings`

- **选择**：与 pulls 类似，注册 `GitRepoSettings*` 路由；页面内嵌 `GitRepoShell`，`shellTab=settings`。
- **替代**：browse 再加 tab 查询态 → settings 侧栏与代码树冲突，URL 难表达子页。
- **理由**：子路由清晰，便于二期加 `settings/members`。

### D2. 路由与 helpers

| 名称（示意） | 路径后缀 |
|--------------|----------|
| `GitRepoSettings` | `settings` → redirect `settings/general` |
| `GitRepoSettingsGeneral` | `settings/general` |
| `GitRepoSettingsDanger` | `settings/danger`（删除） |

- 在 `git-repo-routes.js` 增加 `gitRepoSettingsRoute` / `gitRepoSettingsGeneralRoute` 等。
- `git-repo-route-guards`：settings 路由同样需要 `repositoryId` 解析（与 browse/pulls 一致）。
- 非 manage 访问 settings → 拒绝或回退 browse（与现有 manage 门禁一致）。

### D3. 壳 Tab 集合扩展

- `SHELL_TABS` 增加 `settings`；文案 i18n `git.repo.tab.settings`（管理）。
- 仅 `canManageGitRepo`（本 change = 项目 manage）渲染该 Tab；无权限不展示、深链拦截。

### D4. 基本信息字段范围

- **选择**：对齐列表现有编辑对话框已暴露字段（名称、slug、可见性、默认分支、备注/介绍等），不借机加「语言 / 主页」等码云字段。
- **替代**：一次对齐截图全部字段 → 无后端列、范围膨胀。
- **理由**：一期「迁入口」而非扩模型。

### D5. 删除页独立 vs Danger Zone

- **选择**：侧栏两项「基本信息」「删除仓库」；删除页强确认（输入 slug / 二次确认，沿用或加强现有交互）。
- **替代**：全部堆在 general 底部 → 侧栏只剩一项，与「管理中心」预期略弱。
- **理由**：与探索决议「两项」一致。

### D6. 列表设置入口

- **选择**：列表/精选卡「设置」改为 `router.push` 到 `settings/general`；移除（或停用）原编辑对话框，避免双 UI。
- **删除**：列表上若仍有删除按钮，可保留快捷入口或一并导向 `settings/danger`（实现时二选一，优先单一真相：settings）。

### D7. 灰显「仓库成员管理」

- **选择**：侧栏渲染禁用项 + tooltip「即将推出」；不注册可点路由。
- **理由**：产品决议占位，避免空链。

### D8. 后续仓级 ACL（决议，本 change 不实现）

判定：

```
若 user 在该仓 ACL 有行 → 仅用该行角色（read | write | admin）
否则若为项目成员 → 现有项目角色映射
否则 → 拒绝（公开仓只读规则另遵现有 visibility）
```

覆盖语义：项目 admin 被配成仓 `read` → **不能**删仓 / force-push / 改设置 / 管 ACL。

角色三档：`read`（pull）、`write`（pull+push）、`admin`（manage 含 force-push、设置、ACL）。

谁可改 ACL：`project.create-by` | `project.admin` | 仓 `admin`（均受「有 ACL 行则覆盖」约束）。

引导：创建仓库时自动写入创建者 `admin` 行。

可邀请非项目成员进入仓 ACL。

二期实现时：`GitRepositoryAccessService` 改为按 repoId 的 effectivePerm；REST 与 Smart HTTP 共用；管理 Tab 可见性改查 effective manage。

建议二期约束：禁止移除最后一位仓 `admin`。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 列表与 settings 双入口漂移 | 删除对话框编辑，只留跳转 |
| slug 在 settings 中改名后 URL 失效 | 保存成功后按新 slug `replace` 到 settings/general |
| 一期仍按项目 manage，与已决议 ACL 不一致 | design 标明；二期切换；灰显提示成员未上线 |
| 非 manage 收藏 settings URL | 路由守卫 + 403/回退 |

## Migration Plan

- 无 Flyway（无新表）
- 纯前端路由/UI + 复用 API；回滚 = 恢复列表对话框、去掉 settings Tab/路由

## Open Questions

- 无阻塞项。实现时可自选删除确认文案强度（输入 slug vs 二次 MessageBox），与现网删除体验对齐即可。
