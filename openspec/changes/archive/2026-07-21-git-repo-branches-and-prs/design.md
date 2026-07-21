## Context

项目源码仓库（`project-git-repository`）、Smart HTTP（`git-smart-http`）与浏览（`git-repo-browse`，含提交详情/发行版/贡献者）已落地。分支目前只作为浏览页下拉与 `GET .../branches`（`name` + `isDefault`），无独立管理页，也无 Pull Request。产品要求对齐码云式分支页，并一期交付可讨论的 PR（L2）。

约束（探索已确认）：

- 分支页：仓库级独立路由；MVP 仅「默认分支 + 全部 + 搜索 + 最后更新 + ahead/behind」
- 分支写：仅 manage（`project.create-by` | `project.admin`）
- 开 PR：与 push 同权（上表 + `project.develop` | `project.tester`）
- 合 PR：仅 manage
- 讨论：整单评论（L2）；行内评论非门闩
- 技术：继续进程内 JGit；不引入外部 Git 宿主

## Goals / Non-Goals

**Goals:**

- 独立分支页与增强分支 API（最后提交摘要、ahead/behind）
- manage 可新建/删除分支、设置默认分支
- PR：创建、列表、详情、提交与文件 diff、整单讨论、合并、关闭
- browse 页提供进入分支页的导航入口

**Non-Goals:**

- 「我创建的 / 活跃 / 非活跃」分组与已删除分支回收站
- 审批流、保护分支、CI 状态门禁
- squash / rebase 可选策略（本期固定一种合并方式）
- 行内/文件级评论（可后续）；SSH/LFS

## Decisions

### D1 — 两个新能力 + 轻量修改 browse

- **选择**：`git-repo-branches` 与 `git-repo-pull-requests` 分规格；`git-repo-browse` 仅增加导航入口需求。
- **理由**：PR 是独立域；browse 不膨胀成协作规格。

### D2 — 分支列表数据仍以 Git refs 为准，元数据按需计算

- **选择**：`refs/heads/*` 为真相；列表项补充 tip commit 的 author/time（复用 commit 身份匹配）、相对 `default_branch` 的 ahead/behind（JGit `RevWalk` / count）。
- **理由**：避免为每分支建镜像表；搜索在服务端按分支名过滤（小规模足够，大仓可后续加分页）。
- **备选**：分支元数据表 → 推送 hook 同步，本期过重。

### D3 — 分支写操作走 REST + JGit RefUpdate，不经 Smart HTTP

- **选择**：`POST/DELETE .../branches`、`PUT .../default-branch`；创建从指定 ref（默认当前默认分支 tip）建新 ref；删除拒绝删当前默认分支。
- **理由**：与发行版打 tag 一致；manage UI 不依赖本地 git。
- **说明**：develop 仍可通过 `git push` 创建远程分支；UI 删除仅 manage。

### D4 — PR 元数据落库，diff 现算

- **选择**：表 `sys_git_pull_request`（repository_id、number、title、body、source_branch、target_branch、source_sha、target_sha 快照可选、state：open/merged/closed、author_id、merged_by、时间戳）；评论表 `sys_git_pull_request_comment`（pr_id、user_id、body、parent_id 可选用于回复）。
- **理由**：状态与讨论不是 Git 对象；diff/commits 用 JGit 在 source…target 上计算，合并成功后更新 state 与 target ref。
- **备选**：复用通用 `sys_comment` → 需模块类型扩展，PR 专用表更清晰。

### D5 — 权限矩阵

| 操作 | 角色 |
|------|------|
| 读分支增强列表 / 读 PR / 读评论 | pull 并集（与 browse 相同） |
| 开 PR、发评论 | push 并集 |
| 合并 / 关闭他人 PR* | manage |
| 新建/删除分支、设默认分支 | manage |
| 作者关闭自己的 open PR | push（作者本人）亦允许 |

\*关闭：作者可关自己的；manage 可关任意。合并仅 manage。

### D6 — 合并策略固定为 merge commit

- **选择**：服务端三路合并；清洁则创建 merge commit 并快进/更新 `refs/heads/<target>`；冲突则拒绝并返回冲突提示（不写半成品）。
- **理由**：对「有分叉历史」最通用；FF-only 会挡大量真实 PR。
- **备选**：仅 FF → 体验差；squash → 丢中间提交，可后续。

### D7 — 路由与 UI

- 分支页：`/system/git/repo/:repositoryId/branches`（或项目 path 约定下等价路由）
- PR 列表/详情/新建：同仓库下 `/pulls`、`/pulls/new`、`/pulls/:number`
- 分支行「创建 Pull Request」→ 预填 source=该分支、target=默认分支
- browse「分支 N」可点进分支页

### D8 — API 草图（均需项目/仓库鉴权）

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/system/git/repo/{id}/branches`（扩展字段） | pull |
| POST | `.../branches` | manage |
| DELETE | `.../branches/{name}` | manage |
| PUT | `.../default-branch` | manage |
| GET/POST | `.../pulls` | pull / push |
| GET/PATCH | `.../pulls/{number}` | pull / push（字段限制） |
| GET | `.../pulls/{number}/commits` | pull |
| GET | `.../pulls/{number}/files` | pull |
| POST | `.../pulls/{number}/merge` | manage |
| POST | `.../pulls/{number}/comments` | push |
| GET | `.../pulls/{number}/comments` | pull |

（具体 path 实现时可与现有 `SysGitBrowseController` 风格对齐。）

## Risks / Trade-offs

- **[Risk] ahead/behind 与 diff 在大仓上昂贵** → 列表可限制返回字段；PR diff 复用提交详情的截断策略。
- **[Risk] 裸仓服务端合并实现复杂** → 抽专用 helper + 单测覆盖清洁合并与冲突；失败不更新 ref。
- **[Risk] push 创建的分支与 UI 创建并存** → 列表统一读 refs，无问题；删除权限仅 manage，避免误删。
- **[Trade-off] 无保护分支** → manage 合并即可改默认分支历史；靠角色信任，后续再加规则。
- **[Trade-off] 整单评论无行内** → L2 讨论可用，代码级批注留待下期。

## Migration Plan

1. Flyway（h2 + mysql）建 PR / 评论表；分支无新表。
2. 后端分支扩展与写 API → 分支页 UI。
3. PR API + 合并 helper → PR UI + 分支页入口。
4. browse 导航入口。
5. 回滚：隐藏路由与按钮；表可保留；已合并的 Git 历史不回滚。

## Open Questions

- 无（开 PR 同 push、合 PR 仅 manage、L2 整单讨论、分支 MVP 范围已确认）。行内评论明确为后续增强。
