## Context

当前 MCP 接入（见 `personal-access-token-oauth`、`mcp-server` 两个已实现未归档的变更）：

- 凭证两种：个人访问令牌 `c2b_pat_`（个人中心手动生成，`source=manual`）和 OAuth 访问令牌 `c2b_oat_`（平台自带授权服务器签发）。两者都存在 `sys_user_access_token`，只认「人」，不认项目。
- 鉴权链路：`AccessTokenAuthMechanism`（只拦 MCP 路径）→ `AccessTokenIdentityProvider` → `AccessTokenResolver.authenticate` 返回 `LoginUser`，放进身份属性 `LOGIN_USER_ATTRIBUTE`。
- 项目定位：每个工具带 `project` 参数，`McpContext.scope(connection, projectRef, ...)` 解析成项目 ID，再按「角色权限 ∩ 项目 MCP 模块」校验（ADR-014）。通用通道 `call_operation` 经回环转发网页接口（ADR-015）。
- 授权确认页 `views/oauth/consent.vue` 只有「允许 / 拒绝」；未登录时前端路由守卫会先跳登录页再回来，账号密码登录这一步已经存在。
- 前端项目选项页按目录自动加载卡片，`McpCard.vue` 是独立卡片；`AICard.vue` 是「AI 管理」卡片。弹窗模块名用 `$t('project-mcp.module.' + key)`，但后端 `availableModules` 返回的是 `{key, permissionPrefixes}` 对象，导致显示 `[object Object]`。

## Goals / Non-Goals

**Goals:**

- AI 客户端只经 OAuth（浏览器里账号密码登录）接入，用户不接触令牌。
- 授权时选定团队和项目；令牌绑定该项目；MCP 工具不再需要也不能指定项目。
- MCP 设置入口并入「AI 管理」卡片；模块名按当前语言显示。

**Non-Goals:**

- 不做「已授权客户端」管理页（用户选择直接去掉个人中心令牌页）；撤销授权靠客户端登出（`/oauth/revoke`）、令牌自然过期、或管理员停用账号。
- 不支持一次授权多个项目，也不支持授权后在 MCP 会话内切换项目。
- 不改 OAuth 协议端点的形态（注册、PKCE、刷新轮换、元数据都不变），不改 ADR-014 的权限交集规则。
- 不改 Open API（`/api/**`，项目 API Key）。

## Decisions

### 1. 删除个人访问令牌，仅保留 OAuth

- `AccessTokens` 去掉 `c2b_pat_` 前缀常量与生成入口；`AccessTokenResolver.authenticate` 只接受 `source=oauth` 的行，其它一律返回 `null`（统一 401）。
- 删除 `UserAccessTokenResource`（`/system/user/access-token`）、`SysUserAccessTokenService` 中手动创建 / 列表 / 吊销方法、前端 `accessToken.vue` 与个人中心 `tokens` 标签；`api/system/access-token.js` 只留 OAuth 确认页用的 `checkOAuthAuthorize` / `decideOAuthAuthorize`，改名为 `api/system/oauth.js`。
- `OAuthService.revoke` 里对 `SOURCE_MANUAL` 的分支去掉。
- 备选：保留 PAT 作为无浏览器环境（CI、远程服务器）的兜底。否决：用户明确只要账号密码登录；这类场景目前没有需求，以后需要可另立 ADR 加回。

### 2. 项目绑定存在令牌行上

- `sys_oauth_code`、`sys_user_access_token` 各加 `project_id BIGINT NULL`。`approve` 写入授权码，`exchangeCode` 与 `refresh` 把它原样写入新的访问 / 刷新令牌。
- `AccessTokenResolver.authenticate` 返回值从 `LoginUser` 扩成包含 `projectId` 的结果（例如 `AccessTokenPrincipal(LoginUser, Long projectId)`），`AccessTokenIdentityProvider` 额外放入身份属性 `AccessTokenIdentities.PROJECT_ID_ATTRIBUTE`。`project_id` 为空的 OAuth 令牌视为无效（401），这样存量令牌会触发客户端重新走 OAuth。
- 备选：把项目放进 OAuth `scope`（如 `project:58`）。否决：客户端发起授权时并不知道项目，项目是在确认页里由用户选的；把选择结果存服务端比让客户端回传 scope 简单且不可篡改。
- 备选：按 MCP 会话（`McpConnection`）记住项目、提供 `select_project` 工具。否决：用户要求「登录后选择」，且会话级状态在 Streamable HTTP 多实例 / 重连下不可靠。

### 3. 确认页的团队 / 项目选择

- `POST /oauth/authorize/check` 在原有客户端信息之外返回可选项目：按团队分组，只含「本人是成员且 MCP 已开启」的项目（系统管理员与网页一致，可选全部已开启 MCP 的项目；`decision` 只接受这个列表里的项目），复用 `McpAccessService.enabledProjectsOf(userId)`，每项含 `projectId`、`teamId`、`teamName`、`teamKey`、`projectName`、`projectKey`。
- `POST /oauth/authorize/decision` 增加 `project_id`；`approve=true` 时服务端再次用 `McpAccessService.access(user, projectId)` 校验成员关系与 MCP 开关，不通过则报错、不签发授权码。`approve=false` 不要求项目。
- 前端先选团队（下拉），再选该团队下的项目（下拉）；只有一个团队 / 项目时自动选中；默认优先选中用户网页当前所在项目（`config.currentProjectId`），若它在可选列表里。无可选项目时禁用「允许」并提示联系项目管理员开启 MCP。

### 4. MCP 工具改为从身份取项目

- `McpContext.scope(connection, anyOf...)` 去掉 `projectRef` 参数，项目 ID 取自身份属性；`resolveProjectId` 删除。项目不存在、不再是成员、MCP 被关闭时报错文案提示「请联系管理员，或在客户端重新登录授权选择其他项目」。
- 所有工具删掉 `@ToolArg(name = "project")` 参数和 `PROJECT_DESC`；工具描述里涉及「先调用 list_my_projects」的文字同步改掉。
- `list_my_projects` → `get_current_project`：返回绑定项目信息、可用模块、项目下代码仓库列表。
- `resolve_project_by_git_remote` 保留，解析出仓库后比对 `projectId`，不是绑定项目则报错；返回仓库信息（便于 AI 把本地仓库对上平台仓库）。
- 通用通道 `call_operation` 已经通过 scope 拿项目 ID 拼路径，改为从 scope 取即可，不需要额外改转发逻辑。

### 5. 前端入口与国际化

- `McpCard.vue` 从卡片目录移出，改为纯弹窗组件 `components/Project/McpSettingsDialog.vue`（或同目录非卡片组件，按项目卡片自动加载规则避开），由 `AICard.vue` 的「MCP 接入」链接打开。`AICard.cardVisible` 改为三个链接任一可见即显示；MCP 链接要求 `system:project:query`。
- 弹窗 `v-for` 改为遍历 `availableModules` 的 `m.key`，标签 `$t('project-mcp.module.' + m.key)`；7 种语言补齐 9 个模块（defect、case、plan、deliverable、report、document、code、dashboard、project）的翻译，缺失的 key 一并补。
- 去掉「项目引用」行、「去个人中心生成令牌」链接；客户端示例只含 URL：Cursor `{"mcpServers":{"cat2bug":{"url":...}}}`，Claude Code `claude mcp add --transport http cat2bug <url>` 后在 `/mcp` 里登录，Codex `[mcp_servers.cat2bug] url = ...` 后执行 `codex mcp login cat2bug`，Claude Desktop / Web 在 Connectors 里填 URL。统一提示「首次连接会打开浏览器登录 Cat2Bug，并选择团队和项目」。

### 6. 数据迁移

新增 `V1_1_0_42__mcp_oauth_project_binding.sql`（H2 `db/migration` 与 MySQL `db/mysql` 各一份）：

1. `sys_oauth_code`、`sys_user_access_token` 加 `project_id`。
2. `DELETE FROM sys_user_access_token WHERE source = 'manual'`。
3. `DELETE FROM sys_user_access_token WHERE project_id IS NULL`（存量 OAuth 令牌全部失效）；`DELETE FROM sys_oauth_code`（未用授权码不绑定项目，直接清掉）。

## Risks / Trade-offs

- [已连接的客户端全部失效] → 客户端收到 401 后会自动重新走 OAuth；在版本记录和弹窗提示里说明需要重新登录一次。
- [无浏览器环境（CI、SSH 远程机）无法接入] → 本次明确不支持；如需要另立 ADR 恢复受限的机器令牌。
- [一次授权只对应一个项目，多项目用户要配多个连接或反复授权] → 接受；客户端可以用不同名字（如 `cat2bug-p58`）各配一个 MCP 连接，弹窗示例里说明。
- [Codex 的 OAuth 登录依赖其 rmcp 客户端] → 示例写明 `codex mcp login`；实测不通时在 tasks 里记录并在弹窗里给出版本要求。
- [鉴权改动面大] → 改 `OAuthService` / `OAuthResource` / `AccessTokenAuthMechanism` 必须跑 `QuarkusOAuthTest`、`QuarkusAccessTokenTest`（ADR-013 副作用条款），并做 `/code-review` 安全审查。

## Migration Plan

1. 部署新版本，Flyway 执行 `V1_1_0_42`，旧令牌清空。
2. 用户在 AI 客户端里删掉旧的 `Authorization` 请求头配置，只保留 URL，重新连接并在浏览器里登录、选项目。
3. 回滚：回退版本后旧代码仍能启动（新增列可空），但已删除的 PAT 无法恢复，用户需重新生成。

## Open Questions

- 无。

## 相关长期文档

- `docs/03-技术方案/MCP接入与权限.md`
- `docs/03-技术方案/权限模型与鉴权链路.md`
- `docs/05-接口文档/接口清单-OpenAPI与AI.md`
- `docs/02-系统架构/数据库.md`
- `docs/01-产品需求/产品定位与核心功能.md`
- `docs/07-技术决策/ADR-013-AI客户端以个人身份接入-个人令牌加自带OAuth.md`（将被取代）
- `docs/07-技术决策/ADR-014-MCP权限为角色权限与项目模块的交集.md`
- `docs/07-技术决策/ADR-015-MCP用精做工具加通用通道-经回环转发复用网页接口.md`

拟新增 ADR：`ADR-016-AI客户端只经OAuth登录接入且授权绑定单个项目`（取代 ADR-013）。
