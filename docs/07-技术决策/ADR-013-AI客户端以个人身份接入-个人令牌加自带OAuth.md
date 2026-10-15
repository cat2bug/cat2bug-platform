# ADR-013 AI 客户端以个人身份接入：个人访问令牌 + 平台自带 OAuth，令牌只用于 MCP

约束以后所有「让 AI 客户端（Cursor、Claude Code、Claude Desktop 等）访问 cat2bug」的入口：身份是用户本人，凭证只在 MCP 路径生效。

- 日期：2026-10-15
- 状态：被 [[ADR-016-AI客户端只经OAuth登录接入且授权绑定单个项目]] 取代（个人访问令牌已删除；自带 OAuth、令牌只用于 MCP 两条沿用）

## 背景

要让 AI 客户端通过 MCP 读写缺陷、用例，并能执行通过 / 驳回等动作。现有凭证都不合适：网页 JWT 4 小时过期且绑定浏览器会话；Open API Key 是项目级的，看不出是谁操作，也无法按个人角色限制权限。MCP 授权规范要求 MCP 服务器作为 OAuth 受保护资源，Claude Desktop / claude.ai 只支持 OAuth，Cursor、Claude Code 等支持在请求头里直接填令牌。依据：`openspec/changes/personal-access-token-oauth/design.md`。

## 候选方案

### 方案 A：复用项目 API Key

优点：已有实现。

缺点：不是个人身份，审计和权限无法落到人；Claude Desktop / 网页版无法使用（只支持 OAuth）。

### 方案 B：接入外部 IdP（Keycloak 等）

优点：OAuth 实现成熟。

缺点：多一个部署组件，违背单文件交付（[[ADR-004-Native交付分两道门并接受约38MiB]]）；用户体系要同步。

### 方案 C：个人访问令牌 + 平台自带最小 OAuth 授权服务器

优点：身份就是用户本人，权限沿用角色；两类客户端都能接入；不增加部署组件。

缺点：OAuth 端点要自己实现并承担安全责任。

## 最终决定

采用方案 C：

- 用户在个人中心自助生成 / 吊销个人访问令牌（`c2b_pat_`）；平台自带 OAuth 2.1 授权服务器（动态客户端注册、授权码 + PKCE S256、刷新令牌轮换与重放吊销），签发 `c2b_oat_` 访问令牌。两者存同一张表 `sys_user_access_token`，只存哈希。
- 令牌**只在 MCP 路径**生效，网页接口 `/system/**` 不接受；网页仍只用 JWT。
- 令牌不带独立权限范围：实际权限 = 用户角色权限 ∩ 项目 MCP 设置。
- 不做令牌形式的长期 JWT（无法单独吊销）。

## 原因

- 「谁做的」必须能落到具体用户，个人令牌与 OAuth 都满足；项目 Key 做不到。
- 只开放 MCP 路径，泄露后影响面限定在为 AI 场景设计过的接口内。
- 所需 OAuth 端点有限，按 RFC 7591 / 7636 / 8414 / 7009 逐条写测试即可控，不值得为此引入外部组件。

## 副作用

- OAuth 实现由我们维护：改动 `OAuthService`、`OAuthResource`、`AccessTokenAuthMechanism` 时必须跑 `QuarkusOAuthTest`、`QuarkusAccessTokenTest` 并做安全审查。
- 反向代理部署要配置 `cat2bug.public-url`，并原样转发 OAuth / `.well-known` / MCP 路径，见 [[部署]]。
- 新增给 AI 用的能力应做成 MCP 工具，而不是放宽令牌去访问网页接口。
- 链路细节见 [[权限模型与鉴权链路]]。
