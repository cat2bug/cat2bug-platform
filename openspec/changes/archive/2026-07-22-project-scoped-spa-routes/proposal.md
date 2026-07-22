## Why

项目业务页仍散落在 `/defect`、`/project/git-repo/...` 等与团队/项目无关的路径上，分享链接虽已有 `/{teamKey}/{projectKey}`，但进入业务后地址栏立即丢失命名空间；且前端使用 Hash 路由（`#/...`），嵌入式与 nginx 部署的真实 URL 也不统一。需要把**项目业务页**统一为可读路径 `/{teamKey}/{projectKey}/{module}/**`，并改为 History 模式（无 `#`）。

## What Changes

- 前端改为 **History 路由**（无 URL hash）；嵌入式 `EmbeddedSpaFallbackFilter` 与 nginx `try_files` 继续承担 SPA fallback，并排除 `/{team}/{project}/code/{slug}.git/**`。
- **项目业务页**挂到 `/{teamKey}/{projectKey}/{module}/**`；第三段为单数模块白名单（与现有菜单对齐）：`dashboard`、`defect`、`case`、`plan`、`report`、`document`、`module`、`notice`、`member`、`option`、`api`、`tools`、`code` 等。
- 源码浏览：`/{teamKey}/{projectKey}/code/{slug}/**`（SPA）；clone 仍为已有 `.../code/{slug}.git/**`（Smart HTTP）。
- **切换项目时 URL 前缀必变**（解析 path keys → 切换当前项目上下文）。
- 旧业务路径（如 `/defect`、`/project/git-repo/...`）提供 **redirect** 到新路径（过渡期）。
- 系统/认证页（`/login`、`/setup`、`/system/...` 等）**不迁入**项目前缀。
- **BREAKING（前端书签/深链）**：Hash 深链与旧业务 path 不再是规范地址；需依赖 redirect 或重新打开。
- REST / Smart HTTP 业务 API **本 change 不改路径**（clone URL 已 namespaced，保持现状）。

## Capabilities

### New Capabilities

- `spa-history-routing`: 全站 History 模式、无 `#` 的规范 URL；嵌入式与 nginx 行为一致；SPA fallback 与 Git/API 路径隔离。
- `project-scoped-module-routes`: 项目业务模块挂在 `/{teamKey}/{projectKey}/{module}/**`；模块白名单；切项目改前缀；旧路径 redirect。

### Modified Capabilities

- `project-path-route`: 分享/落地 URL 改为无 hash 的 History 路径；空第三段或 `dashboard` 仍打开项目仪表盘；与模块子路由共存。
- `git-repo-browse`: 浏览/分支/提交/PR/发行版等 UI 深链改为 `/{teamKey}/{projectKey}/code/{slug}/**`（不再以 `/project/git-repo/browse/:repositoryId` 为规范地址）。

## Impact

- **前端**：`createWebHistory`、`permission`/动态菜单 path 改写、`path-key` 分享 URL、路由守卫解析 keys、大量 `router.push` / `activeMenu`、Git 浏览相关视图与 API 封装中的路由名。
- **后端（有限）**：扩展 `EmbeddedSpaFallbackFilter` 排除 namespaced `.git`；一般不改 REST Controller 路径。
- **部署**：nginx 已具备 `try_files`；确认与嵌入式一致；文档/冒烟（如 `native-spa-smoke`）覆盖 history 深链。
- **数据**：可能需 Flyway 调整 `sys_menu.path`（或运行时改写菜单 URL）；存量书签依赖 redirect。
- **依赖**：已有 `team-project-path-key` / `project-path-route` / `git-namespaced-clone-url` 底座。
