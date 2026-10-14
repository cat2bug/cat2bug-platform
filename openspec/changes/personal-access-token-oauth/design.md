## Context

- 现有鉴权链路：`JwtAuthFilter`（网页 JWT，会话存内存或 Redis）→ `PermissionAuthorizationFilter` + `@RequiresPermissions`；Open API 走 `ApiKeyAuthFilter`（项目级 Key）；Git Smart HTTP 走 Basic。见 `docs/03-技术方案/权限模型与鉴权链路.md`。
- 功能权限按「当前团队 + 当前项目」的角色求并集，当前项目存在 `sys_user_config`。MCP 调用会显式指定项目，项目上下文的处理放在 `mcp-server` 变更，本变更只负责「认出是谁」。
- MCP 授权规范（2025-06 及之后版本）要求：MCP 服务器是 OAuth 受保护资源，未授权返回 401 并在 `WWW-Authenticate` 中给出受保护资源元数据地址；授权走 OAuth 2.1，公开客户端必须用 PKCE；客户端通过动态客户端注册（RFC 7591）或客户端元数据文档获得 client_id；请求带 `resource` 参数（RFC 8707）绑定受众。受保护资源元数据端点属于 MCP 端，放在 `mcp-server` 变更。
- 目标客户端：Cursor、Claude Code、Codex CLI、OpenCode 用请求头携带个人访问令牌；Claude Desktop 与 claude.ai 网页版走 OAuth。

## Goals / Non-Goals

**Goals:**

- 用户能自助生成和吊销个人访问令牌，令牌泄露时可立即失效。
- Claude Desktop / 网页版可以通过标准 OAuth 流程连接，用户只需登录并点「允许」。
- 两种令牌统一存储、统一校验、统一在个人中心管理。

**Non-Goals:**

- 不做第三方登录（用 GitHub / 企业微信登录 cat2bug）。
- 不让令牌访问网页接口 `/system/**`。
- 不做令牌级权限范围（scope 只做协议层面的固定值），权限完全来自用户角色与项目 MCP 设置。
- 不做客户端元数据文档（CIMD）方式的注册，先只支持动态客户端注册；见 Open Questions。

## Decisions

### D1：一张令牌表，两种来源

表 `sys_user_access_token`：`token_id`、`user_id`、`name`、`token_hash`（SHA-256）、`token_prefix`（类型前缀 + 随机部分前几位，便于用户辨认）、`source`（`manual` / `oauth` / `oauth_refresh`）、`client_id`（OAuth 来源时）、`grant_id`（同一次 OAuth 授权产生的访问令牌与刷新令牌共用，用于整链吊销）、`resource`、`expires_at`、`last_used_at`、`last_used_ip`、`revoked_at`、`create_time`。

- 令牌格式：`c2b_pat_` + 32 字节随机数的 Base62（手动）；`c2b_oat_`（OAuth 访问令牌）、`c2b_ort_`（刷新令牌）、`c2b_oac_`（授权码）同样是前缀 + 随机数；动态注册的 client_id 为 `c2b_client_` + UUID。前缀让泄露扫描工具能识别，也方便日志脱敏。
- 只存哈希：明文只在创建时返回一次。
- 否决「把令牌做成长期 JWT」：JWT 无法单独吊销，除非再维护黑名单，不如直接查表。

### D2：令牌只在 MCP 路径生效

实现为 Quarkus Security 的 `AccessTokenAuthMechanism` + `IdentityProvider`（而非 JAX-RS Filter，因为 MCP 端点由 quarkus-mcp-server 以 Vert.x 路由提供，不经过 RESTEasy 过滤器）：只处理 `/mcp`、`/mcp/*`，从 `Authorization: Bearer` 读令牌，查表校验未过期、未吊销、用户状态正常后构造 `LoginUser` 放入 identity 属性 `cat2bug.loginUser`（用 `AccessTokenIdentities.loginUser(identity)` 取出），并异步更新 `last_used_at`。`quarkus.http.auth.permission.mcp` 对 MCP 路径要求已认证。`JwtAuthFilter` 对 MCP 路径不处理。

否决「令牌在所有接口都可用」：扩大了泄露后的影响面；网页接口没有为 AI 场景做幂等、限流和审计标记。

### D3：平台自带最小授权服务器

| 端点 | 说明 |
|---|---|
| `GET /.well-known/oauth-authorization-server` | 元数据：issuer、各端点地址、`code_challenge_methods_supported: [S256]`、`grant_types_supported: [authorization_code, refresh_token]`、`token_endpoint_auth_methods_supported: [none]` |
| `POST /oauth/register` | 动态客户端注册，只接受公开客户端；校验 `redirect_uris`（https，或回环地址 `http://127.0.0.1` / `localhost` 任意端口）；记录 `client_name` |
| `GET /oauth/authorize` | 校验参数后 302 到前端确认页 `/oauth/consent?...`；不合法的 client 或 redirect_uri 直接显示错误，不跳回 |
| `POST /oauth/authorize/decision` | 需登录（JWT），用户点允许后生成授权码（一次性，10 分钟），返回带 `code` 与 `state` 的回调地址 |
| `POST /oauth/token` | 授权码 + `code_verifier` 换访问令牌（1 小时）和刷新令牌（30 天）；刷新时轮换刷新令牌，旧刷新令牌再次使用视为泄露并吊销整条链 |
| `POST /oauth/revoke` | 吊销访问令牌或刷新令牌 |

否决「接入 Keycloak 等外部 IdP」：增加部署组件，违背单文件交付（ADR-004）。否决引入完整 OAuth 框架：Quarkus 侧没有轻量的授权服务器扩展，所需端点有限，自己实现可控，按规范逐条写测试。

表：`sys_oauth_client`（client_id、client_name、redirect_uris、create_time、last_used_at）、`sys_oauth_code`（code 哈希、client_id、user_id、redirect_uri、code_challenge、resource、expires_at、used）、刷新令牌存 `sys_user_access_token`（`source=oauth_refresh`，与访问令牌通过 `grant_id` 关联；比 `parent_id` 链式关联更简单，轮换与重放吊销按 `grant_id` 一次更新）。

迁移脚本为 `V1_1_0_40__access_token_oauth.sql`（H2 / MySQL 各一份）：回滚前的备份分支 `backup/core-before-rollback-20261014` 已用过 1.1.0.12～39，部分开发库执行过，沿用 12 会与已有记录冲突。

### D4：issuer 与对外地址

新增 `cat2bug.public-url`。未配置时从请求的 `X-Forwarded-Proto` / `X-Forwarded-Host` / `Host` 推导，并在启动日志和个人中心令牌页给出提示：反向代理场景必须配置，否则客户端可能拿到错误的 issuer。

### D5：授权确认页

前端新增路由 `/oauth/consent`，未登录时先走登录页并带回原地址（顺带修复登录页 `router.push({path})` 丢失 query 的问题，改为 push 完整的站内相对地址，并拒绝 `//` 开头的外部地址）。页面显示「<客户端名称> 请求以你的身份访问 cat2bug」、回调地址的主机名（回环地址标注「本机应用」）、将获得的能力说明（与你在网页上的权限相同，受项目 MCP 设置限制），按钮「允许」「拒绝」。

防点击劫持：确认页检测到自己被嵌入 iframe 时只显示错误、不渲染按钮；单 JAR 由 `EmbeddedSpaFallbackFilter`、Docker 由 nginx 为该页加 `X-Frame-Options: DENY` 与 `Content-Security-Policy: frame-ancestors 'none'`。decision 接口只认请求头里的 JWT（不读 Cookie、未开 CORS），没有 CSRF 面。

### D6：防滥用

- `/oauth/register`、`/oauth/token`、`/oauth/revoke` 按 IP 限流；客户端 IP 由 `IpRateLimiter.clientKey` 取：只有直连地址是回环 / 内网（即经过本机或内网反向代理）时才信任 `X-Forwarded-For` 的最后一段（代理追加的那一段），其次 `X-Real-IP`；公网直连一律用连接地址，防止伪造请求头绕过限流。
- 授权码只能用一次，且必须与 client_id、redirect_uri、code_verifier 全部匹配。
- 令牌校验失败统一返回 401，不区分「不存在 / 过期 / 吊销」。
- 用户被停用或删除时，其所有令牌立即失效（校验时检查用户状态）。

## Risks / Trade-offs

- [自实现 OAuth 存在安全漏洞] → 按 OAuth 2.1 与 RFC 7591 / 7636 / 8707 / 8414 逐条写测试；上线前做一次安全审查（`/code-review`）。
- [动态客户端注册被刷] → IP 限流 + 定期清理 30 天未使用且没有有效令牌的客户端。
- [不同 MCP 客户端对授权规范版本支持不一] → 实施时用 Claude Desktop、Claude Code、Cursor 实测；遇到差异写入踩坑记录。
- [反向代理导致 issuer 不一致] → D4 的配置与提示。

迁移：只新增表和端点，不改现有数据；回滚时删除新端点即可，遗留表无副作用。

## Open Questions

- 是否需要支持客户端元数据文档（Client ID Metadata Documents）作为动态注册的替代？取决于实施时主流客户端的支持情况。
- 访问令牌 1 小时、刷新令牌 30 天是否合适？是否需要在系统配置里可调？

## 相关长期文档

- `docs/03-技术方案/权限模型与鉴权链路.md`（需新增「个人访问令牌与 OAuth」一节）
- `docs/05-接口文档/接口约定.md`、`docs/05-接口文档/接口清单-OpenAPI与AI.md`（需新增令牌与 OAuth 接口）
- `docs/02-系统架构/部署.md`（`cat2bug.public-url` 与反向代理）
- `docs/07-技术决策/ADR-004-Native交付分两道门并接受约38MiB.md`（不引入外部 IdP 的依据）

拟新增 ADR：「AI 客户端以个人身份接入：个人访问令牌 + 平台自带 OAuth，令牌只用于 MCP」。
