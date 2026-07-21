## Why

团队与项目目前只有展示名称，URL 与 Git clone 地址依赖数字 ID（如 `/git/p/{projectId}/{slug}.git`），不便分享与记忆。需要在创建团队/项目时登记唯一英文路径键，使 `/{teamKey}/{projectKey}` 可打开项目，并使源码仓库地址落在同一命名空间下。

## What Changes

- 创建/编辑团队时必填全局唯一英文键 `teamKey`；创建/编辑项目时必填团队内唯一英文键 `projectKey`（展示名仍可中文）
- **产品约定**：项目必须属于团队；本能力不支持「无团队项目」路径
- 浏览器访问 `/{teamKey}/{projectKey}`：解析后切换当前项目，并进入**仪表盘**
- Git Smart HTTP clone URL 改为 `/{teamKey}/{projectKey}/code/{repoSlug}.git`（保留 `.git`）
- **BREAKING**（对已发出的 clone URL）：废弃以 `/git/p/{projectId}/...` 为对外主形态；实现期可短暂兼容旧路径，但文档与 UI 以新路径为准
- `teamKey` / `projectKey` 允许修改，但须强提示旧分享链接与 clone URL 失效（key 别名/301 非本 change MVP）
- 路径段须避开系统保留字（如 `login`、`setup`、`upgrade`、`static`、`git` 等）

## Capabilities

### New Capabilities

- `team-project-path-key`: 团队/项目英文路径键的校验、唯一性、创建编辑 UX 与数据模型
- `project-path-route`: `/{teamKey}/{projectKey}` 分享链接解析、鉴权、切换当前项目并进入仪表盘
- `git-namespaced-clone-url`: 基于 team/project key 的 Git Smart HTTP 路径与 clone URL 生成（替换/演进现有 `/git/p/{id}/` 形态）

### Modified Capabilities

- `git-smart-http`: clone / Smart HTTP 对外路径从 `/git/p/{projectId}/{slug}.git` 演进为 `/{teamKey}/{projectKey}/code/{slug}.git`

## Impact

- **数据**：`sys_team` / `sys_project` 增加 path key 字段与唯一约束；存量数据需迁移策略（生成或强制补录）
- **后端**：团队/项目 API 校验；路径解析 API；Git Controller / Filter / `getCloneUrl()` 改挂新路径
- **前端**：创建/编辑表单；动态路由或入口守卫；仪表盘跳转；源码页展示新 clone URL
- **运维/文档**：对外 clone 与分享链接示例更新；保留字列表
- **非目标**：key 永久别名、无团队命名空间、把全部业务 REST 迁到 `/{team}/{project}/...`
