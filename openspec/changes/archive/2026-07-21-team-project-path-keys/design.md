## Context

Cat2Bug 团队/项目仅有展示名；Git clone 当前为 `/git/p/{projectId}/{slug}.git`。产品希望用可读路径键构成分享链接与源码地址：

- `/{teamKey}/{projectKey}` → 打开项目仪表盘  
- `/{teamKey}/{projectKey}/code/{repoSlug}.git` → Git Smart HTTP  

约束（探索已定）：项目始终属于团队；落地页为仪表盘；展示名可中文；key 可改但须提示断链；key 别名非 MVP。

## Goals / Non-Goals

**Goals:**

- 团队/项目 path key 数据模型、校验、唯一性与创建编辑 UX
- 前端（及必要后端解析）支持分享链接进入仪表盘
- Git 对外 URL 与 Filter/Controller 切换到 namespaced 路径
- 存量团队/项目补齐 key 的迁移策略

**Non-Goals:**

- 把全部业务 REST 迁到 `/{team}/{project}/...`
- key 永久别名 / 301 表（可后续）
- 无团队项目命名空间
- 改变 Git 权限矩阵、Basic 复用登录、JGit 实现本质

## Decisions

### D1 — Key 字段与唯一性

- **选择**：`sys_team.team_key` 全局唯一；`sys_project.project_key` 在同一 `team_id` 内唯一。格式对齐仓库 slug（字母数字及 `.-_`，首尾字母或数字）。
- **理由**：GitLab 风格 `group/project`；跨团队可同名 project_key。
- **备选**：project_key 全局唯一（更严，不必要）。

### D2 — 保留字黑名单

- **选择**：`teamKey` / 路径第一段不得等于系统路由前缀：`login`、`register`、`setup`、`upgrade`、`static`、`profile`、`prod-api`、`git`、`system`、`project`、`docs`、`websocket` 等（实现时收敛为常量列表）。
- **理由**：真路由 `/:teamKey/:projectKey` 会与现有路径冲突。

### D3 — 分享链接路由策略

- **选择**：前端增加 `/:teamKey/:projectKey`（及可选子路径守卫）；解析成功后设置当前项目并 `replace`/`push` 到现有仪表盘路由，**地址栏尽量保持** `/{teamKey}/{projectKey}` 或仪表盘仍挂在该前缀下（实现优先：进入仪表盘组件且 URL 保留可读路径）。
- **备选**：仅 302 到旧 dashboard URL（分享感弱，不首选）。
- **鉴权**：须登录且为项目成员（或具备进入该项目的现有权限）；否则登录页/403。

### D4 — Git URL 与旧路径

- **选择**：主路径 `/{teamKey}/{projectKey}/code/{slug}.git`；**短期**保留 `/git/p/{projectId}/{slug}.git` 作为兼容别名（同一 JGit 处理），UI/文档只展示新 URL；下一小版本可删除旧路径。
- **理由**：降低已 clone 用户瞬间全断；同时推动新形态。
- **磁盘**：仍 `{profile}/git/{projectId}/{slug}.git`，不随 URL 变。

### D5 — 存量迁移

- **选择**：Flyway 加可空列 → 启动或管理任务为缺 key 记录生成候选（如拼音/slugify 名称，冲突加后缀）→ 管理端可改；新建必填。
- **备选**：强制管理员手工补齐后才可用分享链接（体验差）。

### D6 — Key 可改

- **选择**：有权限角色可改 teamKey/projectKey；UI 强提示断链；不提供别名表（MVP）。
- **理由**：与仓库 slug 策略一致。

## Risks / Trade-offs

- [路由抢占] → 保留字 + 路由注册顺序（具体路径优先于动态段）
- [改 key 断分享/clone] → 强提示；旧 `/git/p/id` 暂留缓冲
- [存量无 key] → 自动生成 + 可编辑
- [仪表盘挂载复杂] → 若保留可读 URL 成本高，可降级为解析后跳转仪表盘并在页内展示「项目路径」复制（仍满足「能打开」，分享链接触发一次跳转）

## Migration Plan

1. Flyway：h2/mysql 同步加列与唯一索引  
2. 回填 key；团队/项目表单与 API  
3. 路径解析 + 仪表盘入口  
4. Git Controller/Filter/cloneUrl；双轨旧路径  
5. 文档与 TESTING 清单  
6. 回滚：隐藏动态路由、恢复仅展示 `/git/p/...`（列可保留）

## Open Questions

- 仪表盘在现有菜单中的精确 route path（实现时对照 `permission`/动态路由表绑定）
- 兼容 `/git/p/...` 保留几个小版本——默认「至少一个发布周期」
