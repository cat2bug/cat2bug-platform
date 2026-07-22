## Context

Cat2Bug 已具备 `teamKey` / `projectKey`、分享入口 `/{teamKey}/{projectKey}`，以及 Git Smart HTTP `/{teamKey}/{projectKey}/code/{slug}.git/**`。项目业务 SPA 仍主要挂在菜单拼出的 `/defect`、`/plan`、`/project/git-repo/browse/:id` 等路径上，且前端使用 `createWebHashHistory`（URL 含 `#`）。嵌入式侧已有 `EmbeddedSpaFallbackFilter`，nginx 已有 `try_files`，具备 History 落地条件。

本 change 把**项目业务页**统一到 `/{teamKey}/{projectKey}/{module}/**`，并去掉路由 `#`；系统/认证页与 REST API 路径不在本期搬迁范围。

## Goals / Non-Goals

**Goals:**

- History 模式：嵌入式与 nginx 规范 URL 一致、无 `#`
- 项目业务模块白名单路径；切项目时前缀必变
- 源码 SPA：`/code/{slug}/**`；与已有 `.git` Smart HTTP 共存
- 旧业务 path / 旧 git browse URL 的 redirect 过渡
- SPA fallback 明确排除 namespaced `.git` 前缀

**Non-Goals:**

- 将 REST 改为 `/{team}/{project}/...`（本期保持现有 `/system/...` 等）
- 把 `/system`、`/monitor`、`/login`、`/setup` 等纳入项目前缀
- 模块名复数化（`defects` 等）与永久 path-key 别名表
- 改变 Git 权限矩阵或 JGit 实现本质

## Decisions

### D1 — 单 change 覆盖 History + 模块路由

- **选择**：一个 OpenSpec change `project-scoped-spa-routes`，实现上分阶段（先 History + 基础设施，再挂模块，源码模块优先试点）。
- **理由**：二者强耦合（无 `#` 后才有真实 `/{t}/{p}/...`）；拆两个 change 易出现半套 URL。
- **备选**：两个独立 change（可并行文档，但合并实现成本更高）。

### D2 — 第三段单数白名单，不用 `/-/`

- **选择**：`defect` / `case` / `plan` / `report` / `document` / `module` / `notice` / `member` / `option` / `api` / `tools` / `code` / `dashboard`；非法第三段 → 404。
- **理由**：对齐现有 `sys_menu` 相对 path；`code` 已占用且与 clone 一致。
- **备选**：GitLab 式 `/-/defect`（隔离更强，但更冗长、与现菜单差更大）。

### D3 — 动态菜单挂载方式

- **选择**：在 `GenerateRoutes`（或等价层）将「项目功能」类菜单改写/挂到父路由 `/:teamKey/:projectKey`，侧栏 `router-link` 带当前 keys；切换项目时按新 keys **重建或重写** 侧栏绝对 path。
- **理由**：菜单仍存相对 segment，避免一次性改光所有 `sys_menu` 行也可工作；Flyway 改 path 可作为加强项。
- **备选**：仅 Flyway 把菜单改成绝对模板（难表达动态 keys）。

### D4 — 仓库浏览用 slug，API 仍用 id

- **选择**：浏览器 `.../code/{slug}/**`；内部 API 继续 `repositoryId`；进入页时用 slug（+项目）解析 id。
- **理由**：可读 URL；REST 不动。
- **备选**：URL 继续数字 id（违背可读路径目标）。

### D5 — History 与 SPA / Git 分流

- **选择**：`createWebHistory()`；扩展 `EmbeddedSpaFallbackFilter`（及文档化 nginx）排除 `/*/code/*.git`（及既有 `/git/` 等）；Git 客户端不依赖 SPA。
- **理由**：嵌入式已有 fallback；补 namespaced `.git` 是正确性必要条件。
- **备选**：继续 Hash（违背「路径不要 `#`」）。

### D6 — 切项目 = 换前缀

- **选择**：从当前 route 抽出 module+后缀，拼到目标 `/{teamKey}/{projectKey}/...` 并 `replace`/`push`；同时更新 `currentProjectId`。
- **理由**：地址栏即上下文，分享/刷新一致。
- **备选**：只改 store、URL 不变（已否决）。

### D7 — 旧路径 redirect

- **选择**：客户端（或少量服务端）将已知旧 path 映射到新 path；缺 keys/slug 时回项目入口或 404。
- **理由**：降低书签断裂；不承诺无限兼容。
- **备选**：立即删除旧路由（迁移更痛）。

## Risks / Trade-offs

- [SPA 误吃 `.git` 流量] → fallback 排除规则 + 冒烟；Accept 头策略保持「仅 HTML」
- [动态菜单/activeMenu 大面积回归] → 先迁 `code` 与 1～2 个列表模块，再铺开；补路由单测/手工清单
- [Hash 书签失效] **BREAKING** → 发布说明；可选短期把 `/#/...` 解析转到 History（若成本可接受）
- [无项目上下文时的业务深链] → 强制先选项目或从 keys 解析；禁止「无 keys 的规范业务 URL」
- [团队设置等非项目页] → 明确不在白名单；保持现有 `/team-options` 等

## Migration Plan

1. History 切换 + 分享 URL / 白名单 path 工具去 `#` + `.git` SPA 排除 + 冒烟  
2. 父路由 `/:teamKey/:projectKey` 与上下文守卫；dashboard 挂载  
3. 改写项目菜单生成；切项目换前缀  
4. 迁移 `code/{slug}/**`（含 commit/PR/release 深链）+ 旧 `/project/git-repo/**` redirect  
5. 迁移其余白名单模块 + 旧根 path redirect  
6. 文档与 TESTING；必要时 Flyway 菜单 path 清理  

**Rollback**：恢复 Hash 与旧 absolute 业务路由（或功能开关）；SPA 排除规则可保留（无害）。

## Open Questions

- Hash 旧链（`/#/defect`）是否做一轮自动跳转 History（建议：有余力则做，非 blocker）
- `sys_menu` 是否用 Flyway 改写 path，还是仅运行时改写（建议：运行时为主，Flyway 可选）
- 集成类菜单（企微/钉钉/飞书/ollama）是并入 `option`/`tools` 还是保留第三段（建议：MVP 并入或暂留旧 path 到下一迭代）
