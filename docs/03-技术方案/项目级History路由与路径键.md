# 项目级 History 路由与路径键

说明前端 History 路由（无 `#`）、团队/项目路径键 `teamKey` / `projectKey` 的规则，以及 `/{teamKey}/{projectKey}/{module}/**` 项目命名空间路由的解析与回退；新增页面、菜单或部署反向代理时参考。

## 概述

- 前端使用 `createWebHistory`，分享链接形如 `https://{host}/{teamKey}/{projectKey}`，不带 `#`。
- 每个团队有全局唯一的 `teamKey`，每个项目有团队内唯一的 `projectKey`；项目业务页统一挂在 `/{teamKey}/{projectKey}/{module}/**` 下，系统/登录/安装等页面留在命名空间外。
- 同一套路径键也用于 Git 命名空间 Clone URL（`/{teamKey}/{projectKey}/code/{slug}.git`）和仓库磁盘路径，见 [[Git Smart HTTP与在线浏览]]。

## 关键流程

1. **路径键规则**（前后端一致）：正则 `^[a-zA-Z0-9](?:[a-zA-Z0-9._-]{0,98}[a-zA-Z0-9])?$`，且不得是保留字（大小写不敏感）。保留字包括系统路径段（`login`、`setup`、`upgrade`、`static`、`profile`、`prod-api`、`git`、`system`、`project`、`docs`、`websocket`、`api`、`code` 等）以及会被 SPA 当作一级路由的段（`invite`、`redirect`、`404`、`401`、`tools`、`tool`、`shard`、`monitor`、`admin`、`team`、`team-options`）。
2. **存量回填**：迁移 `V1_1_0_4__team_project_path_keys.sql` 为已有数据回填 `team-{id}` / `project-{id}`，并加唯一索引 `uk_sys_team_team_key`、`uk_sys_project_team_project_key (team_id, project_key)`。
3. **路由匹配**：`/:teamKey/:projectKey` 是最后注册的兜底路由（`meta.projectScoped`），`beforeEnter` 中若 teamKey 是保留字或任一键格式不合法则跳 `/404`；子路由由 `buildProjectScopedChildren()` 按模块白名单生成。
4. **模块白名单**：`dashboard`、`defect`、`case`、`plan`、`report`、`document`、`module`、`notice`、`member`、`option`、`api`、`tools`、`code`。空模块与 `dashboard` 都进项目仪表盘。`sys_menu.path` 的历史值通过 `PROJECT_MENU_PATH_TO_MODULE` 映射（如 `project-member` → `member`）；集成类菜单（ollama、openai、mcp、飞书、钉钉等）暂时保留旧的 `/project/...` 路径。
5. **解析项目**：前端用 `GET /system/project/path/{teamKey}/{projectKey}`（`getProjectByPathKeys`）把路径键解析成项目并切换当前项目上下文；切换项目时跳到目标项目同模块路径，地址栏不得残留旧键。
6. **旧路径重定向**：`/defect`、`/project/defect` 等旧菜单根，以及 `/project/git-repo/**` 旧浏览地址，由 `buildLegacyProjectModuleRedirectRoutes()` 与 `legacy-project-module-redirect.js` 转到命名空间路径；上下文不足时落到安全入口或明确报错。
7. **服务端回退**：
   - 内嵌部署：`EmbeddedSpaFallbackFilter` 对「GET + `Accept` 含 `text/html`」的未知路径返回 `index.html`；排除 `/static/`、`/profile/`、`/api/`、`/git/`、`/q/`、`/websocket/` 等前缀和 namespaced `.git` 路径；`/login`、`/register` 这类与 POST 接口同路径的页面 GET 一律回退，避免 405。
   - 独立 nginx：`deploy/docker/nginx.conf` 用 `try_files $uri $uri/ /index.html`，API 走 `/prod-api/` 反代。

## 关键代码位置

- `cat2bug-platform-ui/src/router/index.js`（`/:teamKey/:projectKey`、旧路径重定向）
- `cat2bug-platform-ui/src/utils/path-key.js`（`PATH_KEY_PATTERN`、`PATH_KEY_RESERVED`、`buildProjectShareUrl`）
- `cat2bug-platform-ui/src/utils/project-module-path.js`、`legacy-project-module-redirect.js`、`git-repo-routes.js`
- `cat2bug-platform-ui/src/api/system/project.js`（`getProjectByPathKeys`）
- 后端校验：`cat2bug-platform-common/src/main/java/com/cat2bug/common/util/PathKeyValidator.java`
- 迁移：`cat2bug-platform-admin/src/main/resources/db/migration/V1_1_0_4__team_project_path_keys.sql`（MySQL 版在 `db/mysql/`）
- SPA 回退：`cat2bug-platform-admin/src/main/java/com/cat2bug/admin/config/EmbeddedSpaFallbackFilter.java`

## 注意事项

- 新增一级前端路由（如 `/xxx`）时，必须把 `xxx` 同时加入前端 `PATH_KEY_RESERVED` 与后端 `PathKeyValidator` 保留字，否则会被当成 teamKey 解析；已有团队若恰好用了该键需迁移。
- 新增项目模块时同步更新 `PROJECT_MODULES`、`PROJECT_MENU_PATH_TO_MODULE` 和规格中的白名单。
- 改路径键会使旧分享链接与 Clone URL 失效，界面需提示，不做别名跳转。
- nginx 与 Git 共用同一虚拟主机时，不能让 `try_files` 吞掉 `/{team}/{project}/code/{slug}.git/**`，需单独加反代 location。
- 内嵌部署新增后端非 `/system/**` 前缀的 GET 接口时，若浏览器可能以 `Accept: text/html` 访问，要加入 `EXCLUDED_PREFIXES`。

## 相关规格

- `openspec/specs/spa-history-routing/spec.md`
- `openspec/specs/team-project-path-key/spec.md`
- `openspec/specs/project-path-route/spec.md`
- `openspec/specs/project-scoped-module-routes/spec.md`
- `openspec/specs/git-namespaced-clone-url/spec.md`

## 待确认

- 保留字集合里的 `captchaImage` 含大写字母，而前后端比较时都先转小写，导致该项实际不生效（`captchaimage` 可被用作路径键），是否需改为小写。
