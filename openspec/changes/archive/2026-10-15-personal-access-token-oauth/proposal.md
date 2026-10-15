## Why

AI 客户端通过 MCP 操作 cat2bug 时，操作必须落到「具体的人」：权限和网页一致、日志能追溯到人。现有的两种凭证都不合适：网页 JWT 有效期 240 分钟、只适合浏览器会话；Open API Key 绑定项目，不代表个人，也无法套用个人角色权限。另外，Claude Desktop 和 claude.ai 网页版的自定义连接器只支持 OAuth 授权，不能手填请求头，要支持它们，平台必须自己提供 OAuth 授权服务。

## What Changes

- 新增**个人访问令牌**：用户在个人中心生成、命名、设置过期时间、查看最后使用时间、吊销；令牌只显示一次，服务端只存哈希。令牌只代表用户本人，不单独设权限范围。
- 新增 **OAuth 2.1 授权服务器**（面向 MCP 客户端）：
  - 授权服务器元数据（`/.well-known/oauth-authorization-server`）；
  - 动态客户端注册（`/oauth/register`）；
  - 授权端点（`/oauth/authorize`，强制 PKCE S256），复用前端登录页，新增授权确认页；
  - 令牌端点（`/oauth/token`，授权码与刷新令牌，刷新令牌轮换）；
  - 吊销端点（`/oauth/revoke`）。
- OAuth 签发的访问令牌与个人访问令牌存同一张表，在个人中心统一列出（标明来源客户端），可一键吊销。
- 新增配置 `cat2bug.public-url`（平台对外访问地址），用于 OAuth issuer 与回调校验；未配置时按请求的 `Host` / `X-Forwarded-*` 推导。
- 个人访问令牌与 OAuth 访问令牌**只在 MCP 端点生效**，不能用来调用 `/system/**` 等网页接口，网页仍只认 JWT。
- 项目级 Open API Key 保持不变，继续供 CI、Cat2Bug-JUnit 等机器调用。

## Capabilities

### New Capabilities

- `personal-access-token`：个人访问令牌的生成、存储、校验、吊销、过期与使用记录。
- `oauth-authorization-server`：面向 MCP 客户端的 OAuth 2.1 授权服务（元数据、动态客户端注册、授权码 + PKCE、令牌、刷新、吊销、授权确认页）。

### Modified Capabilities

（无）

## Impact

- 数据库：新增令牌表与 OAuth 客户端表、授权码表（H2 与 MySQL 两套 Flyway 脚本）。
- 后端：framework 安全层新增令牌鉴权（只挂在 MCP 路径上）；admin 新增 OAuth 资源和个人令牌管理接口；`PublicPaths` 放行 `/.well-known/**` 与 `/oauth/**` 中无需登录的端点。
- 前端：个人中心新增「访问令牌」页签；新增 OAuth 授权确认页（需登录，未登录先跳登录页再回到确认页）。
- 配置：新增 `cat2bug.public-url`；Docker / 部署文档补充反向代理场景下的配置说明。
- 安全：新增对外暴露的授权端点，需做暴力尝试限制、回调地址严格匹配、授权码一次性与短时效。
- 依赖：本变更不依赖 Quarkus 升级，可与 `upgrade-quarkus-lts` 并行；`mcp-server` 依赖本变更。
