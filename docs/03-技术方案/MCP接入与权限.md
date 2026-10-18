# MCP 接入与权限

AI 客户端（Cursor、Claude Code、Codex、OpenCode、Claude Desktop / 网页版）通过 MCP 以**用户本人**身份读写 cat2bug：只填 MCP 地址，首次连接在浏览器用账号密码登录并选择团队和项目，**一次授权只能操作所选项目**。本文记录端点、鉴权、有效权限、工具与通用通道、审计与各客户端配置；凭证与 OAuth 见 [[权限模型与鉴权链路]]，决策见 [[ADR-016-AI客户端只经OAuth登录接入且授权绑定单个项目]]、[[ADR-014-MCP权限为角色权限与项目模块的交集]]、[[ADR-015-MCP用精做工具加通用通道-经回环转发复用网页接口]]。

## 端点与鉴权

- 扩展 `io.quarkiverse.mcp:quarkus-mcp-server-http`（版本见根 pom `quarkus.mcp.version`，当前 1.9.1），Streamable HTTP，地址 `{cat2bug.public-url}/mcp`。服务名、说明文字、工具分页（`tools.page-size=100`）在 `application.properties` 的 `quarkus.mcp.server.*`。
- 鉴权：`AccessTokenAuthMechanism` 只认 MCP 路径上的 OAuth 访问令牌（`c2b_oat_`，且必须带 `project_id`）；工具里通过 `SecurityIdentity` 的属性 `cat2bug.loginUser` 取用户（`McpContext.caller()`）、`cat2bug.projectId` 取授权项目（`McpContext.projectId()`）。
- 未带或无效令牌：401，`WWW-Authenticate: Bearer realm="cat2bug", resource_metadata="{base}/.well-known/oauth-protected-resource"`。
- 受保护资源元数据 `GET /.well-known/oauth-protected-resource`（及 `/mcp` 子路径）：`resource={base}/mcp`、`authorization_servers=[{base}]`。
- OAuth 令牌绑定的 `resource` 必须等于 `{base}/mcp`（忽略大小写和末尾斜杠）；任一方为空时放行。
- 限流：每用户 60 秒滑动窗口，默认 120 次（`cat2bug.mcp.rate-limit.per-minute`），超限返回工具错误。

## 项目 MCP 设置

- 表 `sys_project_mcp`（`project_id` 主键、`enabled`、`modules` JSON 数组、`update_by`、`update_time`）。**没有行 = 关闭**：老项目升级后默认关闭；新建项目在 `SysProjectService.insertSysProject` 中写入「开启 + 当时全部模块」。
- 存「已勾选列表」：以后在 `McpModules.ALL` 末尾新增模块，已有项目默认不勾选。
- 接口 `GET/PUT /system/project/{projectId}/mcp`（`ProjectMcpResource`）：读要求 `system:project:query`，改要求 `system:project:edit`，且都按**目标项目**的角色权限再校验一次（不依赖网页当前项目）；修改写操作日志「项目MCP设置」。返回值带 `editable`、`mcpUrl`、`publicUrlConfigured`、`teamKey`、`projectKey`。
- 前端：项目设置页「AI 管理」卡片里的「MCP 接入」链接（要求 `system:project:query`），打开弹窗 `views/system/project/option/dialog/McpSettingsDialog.vue`（放在卡片自动加载目录 `option/item/` 之外），含开关、模块勾选（按 `availableModules[].key` 显示本地化名称）、MCP 地址与客户端示例。

## 有效权限

模块与权限前缀（`cat2bug-platform-common/.../security/McpModules.java`）：

| 模块键 | 权限前缀 |
|---|---|
| `defect` | `system:defect:`、`system:comment:` |
| `case` | `system:case:` |
| `plan` | `system:plan:`、`system:PlanItem:` |
| `deliverable` | `system:module:` |
| `document` | `system:document:` |
| `code` | `system:git:` |
| `dashboard` | `system:dashboard:` |
| `project` | `system:project:query` / `list` / `edit` / `member`、`system:project:defect-field:` |

- **实际权限 = 用户在该项目的角色权限 ∩ 项目已勾选模块的权限**。不在表内的权限（用户、角色、菜单、字典、团队、项目 API Key、新建 / 删除 / 推送项目等）一律视为系统级，MCP 不可用。
- 计算服务 `McpAccessService`（system，`service/mcp/`，结果类型 `McpProjectAccess`）：按项目找团队，调用 `selectRolePermissionByUserId(teamId, projectId, userId)`；结果按（用户，项目）缓存 30 秒。修改项目 MCP 设置时立即清除（`invalidateProject`），成员角色变更最多延迟 30 秒生效。**不读写 `sys_user_config`**，AI 操作别的项目不会切走网页当前项目。
- 每次工具调用：`McpContext.scope(connection, anyOf...)` 对**令牌绑定的项目**校验项目存在、用户是成员、项目开启 MCP、权限命中，否则返回明确的中文错误（项目关闭 MCP 或用户退出项目后，已发令牌的调用即被拒，提示重新授权）。

## 工具

- 项目定位：工具**没有** `project` 参数，统一作用于授权时选择的项目。`get_current_project` 返回该项目、可用模块，有代码权限时附带仓库列表；`resolve_project_by_git_remote` 按本地 `git remote` 地址（`/{team}/{project}/code/{slug}.git` 或旧式 `/git/p/{projectId}/{slug}.git`）找授权项目内的仓库，不属于该项目时报错。
- 精做工具（`cat2bug-platform-admin/.../admin/mcp/`）：
  - 缺陷 `McpDefectTools`：`search_defects`、`get_defect`、`create_defect`、`update_defect`、`comment_defect`、`assign_defect`、`repair_defect`（可带 `commits`）、`reject_defect`、`pass_defect`、`close_defect`、`open_defect`、`list_project_members`。
  - 用例 / 计划 / 交付物 / 文档 / 统计 `McpWorkTools`：`search_cases`、`get_case`、`create_case`、`update_case`、`list_plans`、`list_plan_items`、`update_plan_item`、`list_modules`、`list_documents`、`get_project_stats`。
  - 代码 `McpCodeTools`：`list_repositories`、`list_code_tree`、`read_code_file`、`search_code`、`list_commits`、`get_commit`。
- 工具注解标明只读 / 破坏性 / 幂等；`create_defect`、`create_case`、`comment_defect` 支持 `idempotency_key`（表 `sys_mcp_idempotency`，同一用户同一工具同一键 24 小时内返回首次结果，每小时清理）。
- 缺陷流转在工具层镜像网页按钮的处理人规则；后端 `SysDefectService.requireTransition` 另做状态校验，非法流转 409（与网页一致）。
- `update_defect` 必须带上当前 `defectState` 与 `customFields` 再提交，见 [[缺陷更新接口缺字段会重置状态并清空自定义字段]]。
- 每个 `@Tool.Annotations` 与 `@Prompt` 都必须写 `title`，否则输出 `"title": null`，Cursor 会拒收整张表，见 [[MCP工具注解缺title导致Cursor拒收整张工具表]]。

## 使用指南与引导命令

让 AI 像加载了 Skill 一样了解业务流程，并在每一步之后告诉用户下一步，决策见 [[ADR-017-MCP使用指南以工具加Prompt交付-正文放资源文件按项目实况拼接]]。

- 初始化说明（`quarkus.mcp.server.server-info.instructions`）要求 AI 先调用 `get_guide`，每完成一步告诉用户结果、下一步、由谁来做；并提示修代码前按 `get_guide(topic=code)` 确认仓库与基线，单仓且 remote 对得上时不要反复追问。
- 指南工具 `get_guide`（`McpGuideTools`，只读）：`topic` 取 `overview`（默认）/ `roles` / `defect` / `case` / `plan` / `deliverable` / `document` / `code`。返回「本项目实况」+ 主题正文：
  - 实况按当前用户与授权项目实时生成：项目、用户、项目角色、可用模块、各模块可执行操作（按 `McpProjectAccess.allows` 逐个权限判断，映射表 `McpGuideTools.OPERATIONS`）。`overview` / `roles` 附成员按角色分组（需项目成员查看权限，`roles` 列名字，每组最多 30 人）；`defect` 附启用的内置字段、自定义字段（键、类型、必填、选项）和「处理人包含我」的各状态数量；`plan` 附还有未执行子项的计划。
  - 主题模块不在可用模块里时只返回实况和开启方式，不返回操作步骤。
  - **`code` 主题含仓库 / 基线少打断规则**（软规则，无服务端硬闸）：单仓库且本地 `origin` 能 `resolve_project_by_git_remote` 到该仓时不问仓库；多仓或 remote 对不上必须问；基线默认 `defaultBranch`，说明或确认时列出本地常见分支。`fix_defect` Prompt 与 `get_current_project` 的 `repositoryConfirmHint` 同口径。
- 引导命令（MCP Prompts，`McpGuidePrompts`）：`report_defect`、`fix_defect`、`review_defect`、`run_test_plan`、`write_test_cases`、`project_status`，参数全部可选。只生成分步指令（先读指南 → 定位对象 → 写操作前确认 → 每步汇报），不读写数据，权限仍由工具校验。
- 正文是 Markdown 资源：`cat2bug-platform-admin/src/main/resources/mcp-guide/zh-CN/<topic>.md`、`prompts/<name>.md`（`{{参数}}` 占位，空值替换为「（未提供）」），由 `McpGuideContent` 读取并缓存。目录已加入两处 `quarkus.native.resources.includes`。
- **改缺陷流转规则（`McpDefectTools` 的处理人校验、`SysDefectService.requireTransition`）或默认角色权限时，同步改 `mcp-guide/zh-CN/defect.md`、`roles.md`**；测试对状态表关键句有断言。
- 目前只有简体中文；以后加语言时按 `mcp-guide/<语言>/` 分目录。

## 通用通道

- `list_operations` / `describe_operation` / `call_operation`（`McpGenericTools`），操作目录由 `McpOperationCatalog` 启动时扫描 `com.cat2bug.admin.resource.*` 生成，操作 ID 为「资源名去掉 Resource + 方法名」，如 `defect.list`、`defect.getInfo`。
- 收录：带 `@RequiresPermissions`、权限全部属于已映射模块、返回 JSON、无表单参数。排除：路径含 export / import / upload / download / template / raw / archive / shard 等，`/ai`、`/git/`、`/api/`、`/monitor`、`/tool` 前缀，以及从 `sys_user_config` 读当前项目的接口（`EXCLUDED_TEMPLATES`）。
- **对象归属**：网页接口按 ID 访问时不校验对象属于哪个项目，MCP 层转发前用 `McpObjects` 校验路径、查询、请求体里出现的 ID 都属于目标项目；未知的 `*Id` / `*Ids` 字段直接拒绝；`correlationId` 只接受 `defect_log`。新增可被收录的接口时，若有新的 ID 参数，要在 `OWNED_PARAMS` / `McpObjects` 中补归属校验，否则会被拒绝而不是放行。
- 结果截断到 60000 字符。

## 回环网关

精做工具与通用通道都经 `McpGateway` 转发到同进程的网页接口，权限、数据范围、操作日志、异常映射只有一份：

1. 为本次调用创建内部会话 `LoginUser`：角色、权限按目标项目计算（`contextPermissionsMerged=true`），`operChannel=mcp`、`clientName`（`initialize` 的 `clientInfo.name`）、`mcpProjectId`；TTL 120 秒，调用结束在 `finally` 中删除。
2. 用 `java.net.http` 请求 `http://127.0.0.1:{quarkus.http.port}`（测试时 test-port；`cat2bug.mcp.loopback-url` 可覆盖），带 `X-Forwarded-For`（原客户端 IP）。发送用 `sendAsync` + 限时等待。
3. 非 2xx 或 `code≠200` 转为工具错误，消息取接口 `msg`。
4. **在途回环并发上限**（默认 `min(max(4, 线程池/3), 连接池的一半)`，可用 `cat2bug.mcp.loopback.max-in-flight` 覆盖）：同步回环会占住工作线程，嵌套 REST 还要再占一条工作线程和一条 JDBC 连接。只按线程池限制时（曾到 66），并行工具会把默认连接池占满，令牌校验拿不到连接，`POST /mcp` 变成 HTTP 500，网页接口同时失败。见 [[MCP回环占满工作线程导致Quarkus假死]]、[[MCP回环占满数据库连接池导致全站500]]。

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

只填 MCP 地址，首次连接时客户端打开浏览器 → 账号密码登录 → 授权页选团队和项目 → 允许。换项目需在客户端里重新登录授权（或配多个 MCP 服务）。

- 项目设置弹窗「MCP 接入」展示的地址与示例：在 `cat2bug.public-url` 未配置时，前端会把协议与主机与当前网页对齐（含 HTTPS 页面下把后端误推的 `http` 改成 `https`，以及回环主机名 `localhost` ↔ `127.0.0.1` ↔ `::1`）；OAuth issuer 仍以后端为准，故反向代理场景仍须配置 `cat2bug.public-url`。弹窗约 920px 宽，客户端按「国内 / 国际」两行横向标签一眼铺开，下方为说明与配置片段（右上角复制图标）。
- Cursor（`~/.cursor/mcp.json` 或项目 `.cursor/mcp.json`）：`{"mcpServers":{"cat2bug":{"url":"https://<host>/mcp"}}}`，然后在 MCP 设置中点登录。
- VS Code / GitHub Copilot（工作区 `.mcp.json`，或 `.vscode/mcp.json` 的 `servers`）：`type: http` + `url`。
- Trae（`.trae/mcp.json` 或设置 → MCP）、腾讯 CodeBuddy（`~/.codebuddy/.mcp.json` / IDE MCP 页）：`mcpServers` + `type: http` + `url`。
- DeepSeek Harness（dsh）：profile 的 `cordis.patch.yml` 挂载 `@deepseek-ai/dsh-mcp-client`，`transport: streamable-http` + `url`。
- 通义灵码 / Qoder：设置 → MCP 服务，选 SSE/HTTP，填地址。
- 腾讯元器：智能体高级设定 → MCP，自定义插件填地址。
- Claude Code：`claude mcp add --transport http cat2bug https://<host>/mcp`，然后在会话里 `/mcp` 选 cat2bug 登录。
- Windsurf（`~/.codeium/windsurf/mcp_config.json`）、Cline、Continue：均为 `mcpServers` + `url` 形态，见弹窗对应 Tab。
- Codex（`~/.codex/config.toml`）：`[mcp_servers.cat2bug]` 下 `url = "https://<host>/mcp"`，然后 `codex mcp login cat2bug`。
- Claude Desktop / claude.ai：「设置 → Connectors → 添加自定义连接器」填 `https://<host>/mcp`，按提示登录。

反向代理必须配置 `cat2bug.public-url` 并转发 `/mcp`、`/.well-known/*`、`/oauth/*`，见 [[部署]]。

## 测试

`QuarkusMcpServerTest`（含测试用最小客户端 `McpTestClient`，兼容 JSON 与 SSE 响应）覆盖默认设置、401 与元数据、工具清单（无 `project` 参数）、`get_current_project`、跨项目 git remote 拒绝、缺陷闭环与幂等、提交校验、审计字段、归属拒绝、模块关闭、角色交集、网页当前项目不变、限流、代码检索、resource 不匹配、内部会话不可经 Cookie 使用、使用指南（主题、无权裁剪、按角色列出可执行操作）与引导命令（清单、title、参数替换、未认证 401）。Native 冒烟 `smoke-native-docker.sh` 含 MCP 初始化、工具清单、通用目录与一次只读工具调用。

## 相关笔记

- [[权限模型与鉴权链路]]
- [[Git Smart HTTP与在线浏览]]
- [[整体架构]]
- [[数据库]]
- [[接口清单-OpenAPI与AI]]
