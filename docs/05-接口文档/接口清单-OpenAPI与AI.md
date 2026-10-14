# 接口清单：Open API 与 AI

面向第三方的 Open API（`/api/**`，API Key 鉴权）、项目 API Key 管理、个人访问令牌与 OAuth（AI 客户端接入 MCP），以及 AI 模型/账号、AI 生成缺陷与用例、Cat2AI 代理接口速查；约定见 [[接口约定]]。

资源类目录：`cat2bug-platform-admin/src/main/java/com/cat2bug/admin/resource/`（Open API 在其 `open/` 子目录）。

## Open API 鉴权要点

- 请求头 `CAT2BUG-API-KEY: <项目 API Key>`；需 `cat2bug.api.enabled=true`，否则 HTTP 403「Open API 未启用」。
- Key 绑定项目，其 `features`（JSON，如 `{"defect":["list","create"]}`）控制可用功能；`features` 为空表示不限制，Key 被禁用返回 HTTP 401「API KEY 已禁用」，无功能权限返回 HTTP 403。映射见 `cat2bug-platform-framework/.../security/open/ApiPermissionRegistry.java`（路径中纯数字段归一为 `{id}`）。
- 不在映射表里的接口（如 `/api/deliverable/tree`、`/api/report/defect`）只要 Key 有效即可访问。

## Open API 接口

| 方法 | 路径 | 说明 | 功能权限 |
|---|---|---|---|
| GET | `/api/project` | 当前 Key 所属项目信息（`OpenApiProjectResource`） | `project.info` |
| GET | `/api/member` | 项目成员（`OpenApiMemberResource`） | `project.members` |
| GET | `/api/defect` | 缺陷列表（`OpenApiDefectResource`） | `defect.list` |
| GET | `/api/defect/{defectNumber}` | 按项目内编号查缺陷 | `defect.query` |
| POST/PUT | `/api/defect` | 新建 / 编辑缺陷 | `defect.create` / `defect.update` |
| POST | `/api/defect/{defectNum}/{assign\|reject\|repair\|pass\|close\|open}` | 缺陷流转 | `defect.assign` 等同名动作 |
| GET | `/api/case`、`/api/case/{caseNum}` | 用例列表 / 详情（`OpenApiCaseResource`） | `case.list` / `case.query` |
| POST/PUT | `/api/case` | 新建 / 编辑用例 | `case.create` / `case.update` |
| DELETE | `/api/case/{caseNum}` | 删除用例 | `case.delete` |
| GET | `/api/deliverable` | 交付物子级列表（`parentDeliverablePath` 指定父级，缺省为根）（`OpenApiDeliverableResource`） | `deliverable.list` |
| GET | `/api/deliverable/tree` | 全部交付物扁平全路径 | 未映射 |
| GET | `/api/deliverable/info` | 按全路径或名称查交付物 | `deliverable.query` |
| POST | `/api/deliverable` | 新增交付物 | `deliverable.create` |
| POST | `/api/file/upload` | 上传附件（`OpenApiFileResource`） | `file.upload` |
| POST | `/api/report/defect` | 推送缺陷报告（`OpenApiReportResource`；调用方是否为 Cat2Bug-JUnit/JLog「待确认」） | 未映射 |

`DELETE:/api/defect` 在映射表中存在，但 Quarkus 资源类没有对应方法（是否有意不提供「待确认」）。

## 项目 API Key 管理 `ProjectApiResource`（`/system/api`，JWT）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/system/api/list`、`/system/api/{apiId}` | Key 列表 / 详情 | `system:api:list` / `:query` |
| POST/PUT | `/system/api` | 新建 / 编辑（含 `features`、启用状态） | `system:api:add` / `:edit` |
| DELETE | `/system/api/{apiIds}` | 删除 | `system:api:remove` |
| POST | `/system/api/export` | 导出 | `system:api:export` |

## 个人访问令牌（`UserAccessTokenResource`，`/system/user/profile/tokens`，JWT）

只操作当前登录用户自己的令牌，无权限注解。令牌只能用于 MCP 路径，鉴权链路见 [[权限模型与鉴权链路]]。

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/system/user/profile/tokens` | 本人未吊销的令牌：个人令牌（`manual`），以及每个 OAuth 授权各一条（以其刷新令牌 `oauth_refresh` 代表，短期访问令牌不列出） |
| POST | `/system/user/profile/tokens` | 创建个人令牌：`name`（≤ 128 字）、`expireDays`（1～3650，空为永不过期）；明文 `token` 只在本次响应返回 |
| DELETE | `/system/user/profile/tokens/{tokenId}` | 吊销；OAuth 条目按 `grant_id` 连带吊销同一授权的访问令牌与刷新令牌 |

## OAuth 2.1 授权服务器（公开路径，HTTP 状态码按 OAuth 规范返回）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/.well-known/oauth-authorization-server` | 授权服务器元数据（RFC 8414），issuer 取自 `cat2bug.public-url` 或请求头 |
| POST | `/oauth/register` | 动态客户端注册（RFC 7591），只接受公开客户端；成功 201；IP 限流，超限 429 |
| GET | `/oauth/authorize` | 校验 client_id、redirect_uri、`code_challenge_method=S256` 等后 302 到前端 `/oauth/consent`；client 或 redirect_uri 不合法时直接显示错误，不跳回 |
| POST | `/oauth/authorize/check` | 确认页用（JWT）：校验参数并返回客户端名、回调主机 |
| POST | `/oauth/authorize/decision` | 确认页用（JWT）：`approve=true` 生成授权码，否则 `error=access_denied`；返回回调地址 `redirect` |
| POST | `/oauth/token` | 表单提交；`grant_type=authorization_code`（需 `code_verifier`）或 `refresh_token`（轮换）；错误 400 + `error`（如 `invalid_grant`）；IP 限流 |
| POST | `/oauth/revoke` | 吊销访问令牌或刷新令牌（RFC 7009），总是返回 200；IP 限流 |

## AI 模型与账号（JWT）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/system/ai/list` | AI 配置列表（`AiResource`） | `system:ai:list\|system:case:add` |
| GET | `/system/ai/project-model-options` | 项目可选模型 | `system:ai:list\|system:case:add\|system:defect:add` |
| GET | `/system/ai/model/list/default` | 默认模型列表 | `system:ai:list` |
| POST/DELETE | `/system/ai/model` | 下载 / 删除本地模型（Ollama） | `system:ai:add` / `:remove` |
| GET | `/system/ai/{aiId}` | 配置详情 | `system:ai:query` |
| POST/PUT | `/system/ai` | 新增 / 编辑配置 | `system:ai:add` / `:edit` |
| DELETE | `/system/ai/{aiIds}` | 删除配置 | `system:ai:remove` |
| POST | `/system/ai/export` | 导出 | `system:ai:export` |
| GET | `/system/ai/account/list` | AI 账号列表（`AiAccountResource`） | `ai:account:list\|system:case:add` |
| GET | `/system/ai/account/{accountId}` | 账号详情 | `ai:account:query` |
| POST/PUT | `/system/ai/account` | 新增 / 编辑账号 | `ai:account:add` / `:edit` |
| DELETE | `/system/ai/account/{accountIds}` | 删除账号 | `ai:account:remove` |
| POST | `/system/ai/account/test/{accountId}` | 测试连接 | `ai:account:edit` |
| POST | `/system/ai/account/export`、`/ai/account/export` | 导出（后者为前端兼容别名 `AiAccountExportAliasResource`） | `ai:account:export` |

启动时：`cat2bug.ai.enabled=true` 且配置了 `cat2bug.ai.default-business-model` 时，`OllamaDefaultModelBootstrap` 在后台线程查询 Ollama 的 `/api/tags`，缺少该模型就按 `/system/ai/model` 同样的方式流式拉取（进度经 WebSocket 主题 `aiModelPull` 推送）；Ollama 不可达只记一条 WARN，不重试、不阻塞启动。

## AI 辅助生成（JWT）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/ai/defect` | 一键生成缺陷字段（`AiDefectResource`） | `system:defect:add` |
| POST | `/ai/defect/{title\|module\|type\|member\|version}` | 单独生成标题 / 模块 / 类型 / 处理人 / 版本 | `system:defect:add` |
| POST | `/ai/case/list` | AI 生成用例（`AiCaseResource`） | `system:case:add` |
| POST | `/cat2ai/**` | Cat2AI 反向代理（`Cat2AiProxyResource`，前端如 `/cat2ai/api/case/demand`） | 方法无权限注解，需登录 |

- 用例提示词 `/system/CasePrompt/**` 见 [[接口清单-缺陷与用例计划]]。
- Cat2AI 代理上游由 `cat2bug.proxy.routes.ai.host` / `.prefix` / `.timeout`（秒）配置，见 `cat2bug-platform-admin/src/main/resources/application.properties`。
