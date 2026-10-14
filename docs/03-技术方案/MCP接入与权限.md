# MCP 接入与权限

AI 客户端（Cursor、Claude Code、Codex、OpenCode、Claude Desktop / 网页版）通过 MCP 以**用户本人**身份读写 cat2bug。本文记录端点、鉴权、有效权限、工具与通用通道、审计与各客户端配置；凭证与 OAuth 见 [[权限模型与鉴权链路]]，决策见 [[ADR-013-AI客户端以个人身份接入-个人令牌加自带OAuth]]、[[ADR-014-MCP权限为角色权限与项目模块的交集]]、[[ADR-015-MCP用精做工具加通用通道-经回环转发复用网页接口]]。

## 端点与鉴权

- 扩展 `io.quarkiverse.mcp:quarkus-mcp-server-http`（版本见根 pom `quarkus.mcp.version`，当前 1.9.1），Streamable HTTP，地址 `{cat2bug.public-url}/mcp`。服务名、说明文字、工具分页（`tools.page-size=100`）在 `application.properties` 的 `quarkus.mcp.server.*`。
- 鉴权：`AccessTokenAuthMechanism` 只认 MCP 路径上的个人访问令牌 / OAuth 访问令牌；工具里通过 `SecurityIdentity` 的属性 `cat2bug.loginUser` 取用户（`McpContext.caller()`）。
- 未带或无效令牌：401，`WWW-Authenticate: Bearer realm="cat2bug", resource_metadata="{base}/.well-known/oauth-protected-resource"`。
- 受保护资源元数据 `GET /.well-known/oauth-protected-resource`（及 `/mcp` 子路径）：`resource={base}/mcp`、`authorization_servers=[{base}]`。
- OAuth 令牌绑定的 `resource` 必须等于 `{base}/mcp`（忽略大小写和末尾斜杠）；任一方为空时放行。
- 限流：每用户 60 秒滑动窗口，默认 120 次（`cat2bug.mcp.rate-limit.per-minute`），超限返回工具错误。

## 项目 MCP 设置

- 表 `sys_project_mcp`（`project_id` 主键、`enabled`、`modules` JSON 数组、`update_by`、`update_time`）。**没有行 = 关闭**：老项目升级后默认关闭；新建项目在 `SysProjectService.insertSysProject` 中写入「开启 + 当时全部模块」。
- 存「已勾选列表」：以后在 `McpModules.ALL` 末尾新增模块，已有项目默认不勾选。
- 接口 `GET/PUT /system/project/{projectId}/mcp`（`ProjectMcpResource`）：读要求 `system:project:query`，改要求 `system:project:edit`，且都按**目标项目**的角色权限再校验一次（不依赖网页当前项目）；修改写操作日志「项目MCP设置」。返回值带 `editable`、`mcpUrl`、`publicUrlConfigured`、`teamKey`、`projectKey`。
- 前端：项目设置页的「MCP 接入」卡片（`views/system/project/option/item/McpCard.vue`），含开关、模块勾选、客户端示例、跳转「个人中心 → 访问令牌」。

## 有效权限

模块与权限前缀（`cat2bug-platform-common/.../security/McpModules.java`）：

| 模块键 | 权限前缀 |
|---|---|
| `defect` | `system:defect:`、`system:comment:` |
| `case` | `system:case:` |
| `plan` | `system:plan:`、`system:PlanItem:` |
| `deliverable` | `system:module:` |
| `report` | `system:report:` |
| `document` | `system:document:` |
| `code` | `system:git:` |
| `dashboard` | `system:dashboard:` |
| `project` | `system:project:query` / `list` / `edit` / `member`、`system:project:defect-field:` |

- **实际权限 = 用户在该项目的角色权限 ∩ 项目已勾选模块的权限**。不在表内的权限（用户、角色、菜单、字典、团队、项目 API Key、新建 / 删除 / 推送项目等）一律视为系统级，MCP 不可用。
- 计算服务 `McpAccessService`（system，`service/mcp/`，结果类型 `McpProjectAccess`）：按项目找团队，调用 `selectRolePermissionByUserId(teamId, projectId, userId)`；结果按（用户，项目）缓存 30 秒。修改项目 MCP 设置时立即清除（`invalidateProject`），成员角色变更最多延迟 30 秒生效。**不读写 `sys_user_config`**，AI 操作别的项目不会切走网页当前项目。
- 每次工具调用：`McpContext.scope(connection, project, anyOf...)` 校验项目存在、用户是成员、项目开启 MCP、权限命中，否则返回明确的中文错误。

## 工具

- 项目定位：所有项目内工具都有 `project` 参数（项目 ID 或 `团队标识/项目标识`）。`list_my_projects` 列出已开启 MCP 且本人是成员的项目及可用模块；`resolve_project_by_git_remote` 按本地 `git remote` 地址（`/{team}/{project}/code/{slug}.git` 或旧式 `/git/p/{projectId}/{slug}.git`）定位。
- 精做工具（`cat2bug-platform-admin/.../admin/mcp/`）：
  - 缺陷 `McpDefectTools`：`search_defects`、`get_defect`、`create_defect`、`update_defect`、`comment_defect`、`assign_defect`、`repair_defect`（可带 `commits`）、`reject_defect`、`pass_defect`、`close_defect`、`open_defect`、`list_project_members`。
  - 用例 / 计划 / 交付物 / 报告 / 文档 / 统计 `McpWorkTools`：`search_cases`、`get_case`、`create_case`、`update_case`、`list_plans`、`list_plan_items`、`update_plan_item`、`list_modules`、`list_reports`、`get_report`、`list_documents`、`get_project_stats`。
  - 代码 `McpCodeTools`：`list_repositories`、`list_code_tree`、`read_code_file`、`search_code`、`list_commits`、`get_commit`。
- 工具注解标明只读 / 破坏性 / 幂等；`create_defect`、`create_case`、`comment_defect` 支持 `idempotency_key`（表 `sys_mcp_idempotency`，同一用户同一工具同一键 24 小时内返回首次结果，每小时清理）。
- 缺陷流转在工具层镜像网页按钮的处理人规则；后端 `SysDefectService.requireTransition` 另做状态校验，非法流转 409（与网页一致）。
- `update_defect` 必须带上当前 `defectState` 与 `customFields` 再提交，见 [[缺陷更新接口缺字段会重置状态并清空自定义字段]]。

## 通用通道

- `list_operations` / `describe_operation` / `call_operation`（`McpGenericTools`），操作目录由 `McpOperationCatalog` 启动时扫描 `com.cat2bug.admin.resource.*` 生成，操作 ID 为「资源名去掉 Resource + 方法名」，如 `defect.list`、`defect.getInfo`。
- 收录：带 `@RequiresPermissions`、权限全部属于已映射模块、返回 JSON、无表单参数。排除：路径含 export / import / upload / download / template / raw / archive / shard 等，`/ai`、`/git/`、`/api/`、`/monitor`、`/tool` 前缀，以及从 `sys_user_config` 读当前项目的接口（`EXCLUDED_TEMPLATES`）。
- **对象归属**：网页接口按 ID 访问时不校验对象属于哪个项目，MCP 层转发前用 `McpObjects` 校验路径、查询、请求体里出现的 ID 都属于目标项目；未知的 `*Id` / `*Ids` 字段直接拒绝；`correlationId` 只接受 `defect_log` / `report`。新增可被收录的接口时，若有新的 ID 参数，要在 `OWNED_PARAMS` / `McpObjects` 中补归属校验，否则会被拒绝而不是放行。
- 结果截断到 60000 字符。

## 回环网关

精做工具与通用通道都经 `McpGateway` 转发到同进程的网页接口，权限、数据范围、操作日志、异常映射只有一份：

1. 为本次调用创建内部会话 `LoginUser`：角色、权限按目标项目计算（`contextPermissionsMerged=true`），`operChannel=mcp`、`clientName`（`initialize` 的 `clientInfo.name`）、`mcpProjectId`；TTL 120 秒，调用结束在 `finally` 中删除。
2. 用 `java.net.http` 请求 `http://127.0.0.1:{quarkus.http.port}`（测试时 test-port；`cat2bug.mcp.loopback-url` 可覆盖），带 `X-Forwarded-For`（原客户端 IP）。
3. 非 2xx 或 `code≠200` 转为工具错误，消息取接口 `msg`。

内部会话只在 TCP 对端是回环地址时有效（`LoginUserResolver.fromLoopback`，不看转发头），且不能经 `resolveToken`（Cookie / iframe 场景）使用。

## 修复关联提交

- 表 `sys_defect_commit`；网页 `POST /system/defect/{id}/repair` 与 `repair_defect` 都可带 `commits`（每项 `repositoryId` + `commitSha`，或外部 `commitUrl` + `commitSha`），最多 20 条。
- 托管仓库：须属于本项目，经 `ISysGitBrowseService.getCommitDetail` 校验存在与拉取权限，回填完整 SHA、首行说明、提交时间；外部仓库只校验 SHA 格式与 http(s) 链接。校验失败整个修复失败，状态不变。
- 缺陷日志接口返回 `commits`；前端 `ListDefectLog` 显示并可跳转到提交详情。

## 审计

- `sys_oper_log`、`sys_defect_log` 增加 `oper_channel`、`client_name`；网页操作写 NULL（筛选 `web` 时匹配 NULL）。MCP 会话下没登记规则的写接口由 `OperLogFilter` 的兜底规则记录。
- 前端：缺陷日志显示「经由 MCP（客户端名）」，操作日志页有「渠道」列与筛选。

## 代码检索

`GET /system/git/repo/{repositoryId}/search?q=&ref=&path=&limit=`（`system:git:repo:pull`）：不区分大小写的字面量匹配，只看普通 / 可执行文件；单文件 ≤1MB、最多 2 万个文件、总量 64MB、结果默认 50 最多 200、每行截断 300 字符、查询 ≤200 字符。`search_code` 工具基于它。

## 客户端配置

把 `<token>` 换成「个人中心 → 访问令牌」生成的 `c2b_pat_…`：

- Cursor（`~/.cursor/mcp.json` 或项目 `.cursor/mcp.json`）：`{"mcpServers":{"cat2bug":{"url":"https://<host>/mcp","headers":{"Authorization":"Bearer <token>"}}}}`
- Claude Code：`claude mcp add --transport http cat2bug https://<host>/mcp --header "Authorization: Bearer <token>"`
- Codex（`~/.codex/config.toml`）：`[mcp_servers.cat2bug]` 下 `url = "https://<host>/mcp"`、`bearer_token_env_var = "CAT2BUG_TOKEN"`
- Claude Desktop / claude.ai：「设置 → Connectors → 添加自定义连接器」填 `https://<host>/mcp`，按提示登录授权（OAuth），不需要令牌。

反向代理必须配置 `cat2bug.public-url` 并转发 `/mcp`、`/.well-known/*`、`/oauth/*`，见 [[部署]]。

## 测试

`QuarkusMcpServerTest`（含测试用最小客户端 `McpTestClient`，兼容 JSON 与 SSE 响应）覆盖默认设置、401 与元数据、工具清单、缺陷闭环与幂等、提交校验、审计字段、归属拒绝、模块关闭、角色交集、网页当前项目不变、限流、代码检索、resource 不匹配、内部会话不可经 Cookie 使用。Native 冒烟 `smoke-native-docker.sh` 含 MCP 初始化、工具清单、通用目录与一次只读工具调用。

## 相关笔记

- [[权限模型与鉴权链路]]
- [[Git Smart HTTP与在线浏览]]
- [[整体架构]]
- [[数据库]]
- [[接口清单-OpenAPI与AI]]
