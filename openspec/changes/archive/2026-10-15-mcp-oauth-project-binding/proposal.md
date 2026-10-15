## Why

现在 AI 客户端接入 MCP 有两条路：在个人中心生成个人访问令牌（`c2b_pat_`）手填到请求头，或走 OAuth 登录授权。令牌要用户自己复制、保管、记得吊销，容易外泄；而且令牌只代表「人」，不绑定项目，AI 每次调用工具都要带 `project` 参数（`团队标识/项目标识` 或项目 ID），用户看不懂项目设置里「项目引用」那一行是干什么的。Cursor、Claude Code、Codex、Claude Desktop 都已支持 MCP 的 OAuth 登录，可以统一成「账号密码登录 + 授权时选定团队和项目」。

同时项目选项页上的 MCP 是一张独立卡片，和「AI 管理」分开；设置弹窗里的「开放模块」显示成 `project-mcp.module.[object Object]`，没有按当前语言显示模块名。

本次推翻 ADR-013 中「个人访问令牌」那一半：AI 客户端只能经 OAuth（账号密码登录）接入，平台自带 OAuth 授权服务器的部分保留。将新建 ADR 取代 ADR-013。

## What Changes

- **BREAKING** 删除个人访问令牌：去掉个人中心「访问令牌」页、`/system/user/access-token` 接口和前端 API；MCP 端点不再接受 `c2b_pat_`；迁移脚本删除所有已生成的个人访问令牌。
- **BREAKING** OAuth 授权确认页增加「团队 → 项目」选择，只能选本人是成员且已开启 MCP 的项目；授权码、访问令牌、刷新令牌都绑定所选项目，刷新后仍绑定同一项目。迁移脚本吊销所有未绑定项目的存量 OAuth 令牌，客户端需重新登录授权。
- **BREAKING** MCP 工具严格限定在绑定项目内：所有工具去掉 `project` 参数，自动使用绑定项目；`list_my_projects` 改为 `get_current_project`（返回绑定项目、可用模块和代码仓库）；`resolve_project_by_git_remote` 只在绑定项目内查找仓库，不属于该项目的地址直接报错。要换项目需在客户端重新登录授权。
- 项目选项页删除独立的「MCP 接入」卡片，改为「AI 管理」卡片里的「MCP 接入」链接，点开仍是原设置弹窗；有 `system:project:query` 权限即可看到。
- 设置弹窗修复「开放模块」显示：按模块 key 显示当前语言的模块名（7 种语言补齐）。
- 设置弹窗删除「项目引用」一行和「去个人中心生成令牌」链接；各客户端接入示例改为只填 MCP 地址、首次连接时浏览器登录授权（不再出现令牌占位符）。
- 新增 ADR 取代 ADR-013；同步更新 `docs/` 中关于个人访问令牌、MCP 项目参数的说法。

## Capabilities

### New Capabilities

- `mcp-project-binding`: OAuth 授权时选定项目、令牌绑定项目、MCP 工具只作用于绑定项目。

### Modified Capabilities

- `personal-access-token`: 整个能力移除（个人访问令牌不再存在），「令牌只在 MCP 路径生效、无效令牌统一 401、记录使用」改由 OAuth 访问令牌承担。
- `oauth-authorization-server`: 授权确认页增加项目选择，授权码与令牌绑定项目；OAuth 令牌不再出现在个人中心令牌列表（该列表删除）。
- `mcp-endpoint`: `/mcp` 只接受 OAuth 访问令牌，不再接受个人访问令牌。
- `mcp-tools`: 工具不再接收 `project` 参数，项目由令牌决定；`list_my_projects` 改为 `get_current_project`；按 Git 远程地址定位只在绑定项目内生效。
- `project-mcp-settings`: 设置入口移到「AI 管理」卡片；模块名按当前语言显示；去掉项目引用与令牌引导，接入示例改为 OAuth 登录。

注：以上五个能力目前只存在于未归档的变更 `personal-access-token-oauth`、`mcp-server` 中（尚未同步到 `openspec/specs/`）。本变更须在这两个变更归档并同步规格之后再归档。

## Impact

- 后端：`OAuthService`、`OAuthResource`（check / decision 增加项目参数与可选项目列表）、`AccessTokenAuthMechanism` / `AccessTokenResolver` / `AccessTokens`（去掉 PAT 前缀）、`SysUserAccessToken*`（新增 `project_id`，去掉 manual 来源）、`SysOAuthCode*`（新增 `project_id`）、删除 `UserAccessTokenResource`；MCP：`McpContext`（项目取自身份属性）、`McpProjectTools`、`McpDefectTools`、`McpWorkTools`、`McpCodeTools`、`McpGenericTools`（删 `project` 参数）。
- 数据库：新增 Flyway 迁移 `V1_1_0_42`（H2 `db/migration` 与 MySQL `db/mysql` 各一份）：两表加 `project_id`，删除 `source='manual'` 令牌，吊销无项目的 OAuth 令牌与未使用授权码。
- 前端：`views/oauth/consent.vue`（团队/项目选择）、删除 `views/system/user/profile/accessToken.vue` 及个人中心 tokens 标签、`api/system/access-token.js` 只保留 OAuth 确认相关接口、`McpCard.vue` 改为由 `AICard.vue` 打开的弹窗组件、7 种语言 i18n。
- 测试：`QuarkusOAuthTest`、`QuarkusAccessTokenTest`、`AccessTokenUnitTest`、MCP 相关测试需改写；涉及鉴权，实施后做安全审查。
- 已连接的 AI 客户端需重新登录授权一次。
