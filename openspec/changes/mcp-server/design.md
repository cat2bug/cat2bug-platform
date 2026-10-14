## Context

- 现有接口规模：149 个 REST 资源类、508 个接口方法，其中 417 个带 `@RequiresPermissions`。按权限前缀统计，项目内模块主要是缺陷（51）、Git（37）、项目（27）、报告（22）、用例（19）、仪表盘（16）、计划（8）、文档（5）、交付物（`system:module`，5）。
- 功能权限按用户「当前团队 + 当前项目」的角色求并集，当前项目存在 `sys_user_config`，切换项目会重算并写回会话。见 `docs/03-技术方案/权限模型与鉴权链路.md`。
- 令牌鉴权由 `personal-access-token-oauth` 提供：令牌只在 MCP 路径生效，解析出 `LoginUser`。
- MCP 扩展 `quarkus-mcp-server-http` 1.9.x 在 `upgrade-quarkus-lts` 中已验证可用。
- 备份分支 `backup/core-before-rollback-20261014` 中有 Spring AI 版只读 MCP Server 的工具设计（`Cat2bugMcpServerTools`、`McpSharedReadOnlyToolBridge`），可参考工具命名与描述，代码不可直接复用。

## Goals / Non-Goals

**Goals:**

- 四个 CLI / IDE 客户端用请求头、Claude Desktop / 网页版用 OAuth，均可直连 `/mcp`。
- MCP 中能做的事与用户在网页上能做的事一致（受项目 MCP 设置限制），且只有一套权限逻辑。
- 高频闭环（读缺陷 → 修复回填并关联提交 → 通过 / 驳回）用精做工具，AI 调用准确。

**Non-Goals:**

- 系统级管理操作。
- 文件上传下载、导出 Excel 等二进制操作（通用通道第一阶段不包含）。
- 平台内置 AI 去调用外部 MCP（MCP Client）。
- 推送时按提交说明自动关联缺陷（后续增强）。

## Decisions

### D1：项目 MCP 设置的存储与默认值

表 `sys_project_mcp`：`project_id`（主键）、`enabled`、`modules`（已勾选模块键的 JSON 数组）、`update_by`、`update_time`。

- 没有行 = 不开启。老项目升级不写数据，因此默认关闭。
- 新建项目时在同一事务内写入 `enabled=1` 与当时全部模块。
- 存「已勾选列表」而不是「排除列表」：以后新增模块对已有项目默认不开放，需要管理员明确勾选。
- 修改设置需要项目设置相关权限（与项目设置其他页签一致），并写操作日志。

### D2：模块与权限前缀映射

在代码中维护一张映射表（模块键 → 权限前缀列表 → 显示名），例如：

| 模块键 | 权限前缀 |
|---|---|
| `defect` | `system:defect` |
| `case` | `system:case` |
| `plan` | `system:plan` |
| `deliverable` | `system:module` |
| `report` | `system:report` |
| `document` | `system:document` |
| `code` | `system:git` |
| `dashboard` | `system:dashboard` |
| `project` | 项目设置与成员相关的项目级权限 |

具体前缀在实施时按菜单与接口逐一核对。不在映射表中的权限前缀一律视为系统级，MCP 中不可用。

### D3：按（用户，项目）计算有效权限，不改用户的当前项目

新增服务：根据项目找到所属团队，调用现有 `selectRolePermissionByUserId(teamId, projectId, userId)` 求角色权限，再与项目已勾选模块对应的权限前缀求交集。结果按（用户，项目）短时缓存（例如 60 秒），角色或 MCP 设置变更时清除。

不复用 `sys_user_config` 的当前项目：AI 同时操作别的项目时，不能把用户网页上正在看的项目切走。

### D4：MCP 端点与鉴权

- 端点 `/mcp`（Streamable HTTP），由 `personal-access-token-oauth` 的 `AccessTokenAuthFilter` 鉴权。
- `GET /.well-known/oauth-protected-resource`：`resource` 为 `{public-url}/mcp`，`authorization_servers` 指向本平台 issuer。
- 未带令牌或令牌无效：HTTP 401，`WWW-Authenticate: Bearer resource_metadata="{public-url}/.well-known/oauth-protected-resource"`。
- OAuth 访问令牌的 `resource` 必须与 `/mcp` 匹配。
- 记录 MCP 会话的客户端名称（`initialize` 中的 `clientInfo.name`），用于审计。

### D5：项目定位

每个项目内工具都有 `project` 参数，接受「`teamKey/projectKey`」或项目 ID。另提供：

- `list_my_projects`：列出用户所在、且已开启 MCP 的项目，并给出每个项目已开放的模块。
- `resolve_project_by_git_remote(url)`：按 Git 远端地址找到托管在本平台的仓库及其项目，客户端可以用本地仓库的 `origin` 自动定位。

项目未开启 MCP、用户不是项目成员、或模块未勾选时，工具返回明确的错误说明，不返回数据。

### D6：精做工具

第一阶段清单（名称以实施为准）：

- 上下文：`list_my_projects`、`resolve_project_by_git_remote`、`list_project_members`
- 缺陷：`search_defects`、`get_defect`（含日志、评论、关联提交）、`create_defect`、`update_defect`、`assign_defect`、`repair_defect`（可带提交列表与说明）、`pass_defect`、`reject_defect`、`close_defect`、`reopen_defect`、`comment_defect`
- 用例 / 计划：`search_cases`、`get_case`、`create_case`、`update_case`、`list_plans`、`get_plan`
- 交付物 / 报告 / 文档：`list_deliverables`、`get_deliverable`、`list_reports`、`get_report`、`list_documents`、`read_document`
- 代码：`list_repositories`、`list_tree`、`read_file`、`grep_code`、`list_commits`、`get_commit`

实现上，精做工具调用与对应网页接口**相同的 Service 方法**，并在进入时用 D3 的有效权限检查对应权限串，保证与网页规则一致。每个工具标注 MCP 工具注解（`readOnlyHint` / `destructiveHint` / `idempotentHint`），让客户端按风险提示用户确认。

### D7：通用通道

- 操作清单在启动时生成：扫描 admin 中的 JAX-RS 资源，取方法级 `@Path`、HTTP 方法、`@RequiresPermissions`，以及参数（路径、查询、请求体类型）。只保留权限前缀在 D2 映射表中的操作；排除文件上传下载、导出、WebSocket、Git Smart HTTP。
- `list_operations(project, module)`：返回该项目、该模块中用户有效权限允许的操作（ID、一句话说明、HTTP 方法与路径）。
- `describe_operation(operation)`：返回参数说明；请求体给出字段名、类型和必填信息。
- `call_operation(project, operation, args)`：在服务端以该用户身份转发到原接口。

转发方式：为本次调用创建一个短时（例如 2 分钟）的内部会话，会话的角色和权限按 D3 计算（当前项目为目标项目），然后对本机回环地址发起 HTTP 请求，复用完整的过滤器链（认证、`@RequiresPermissions`、数据范围、分页、日志、异常映射）。内部会话带「来源 MCP + 客户端名」标记，供审计使用；调用结束即销毁。

否决方案：
- 直接通过 CDI 调用资源方法：绕过过滤器链，权限和日志都要重写，等于两套逻辑。
- 每个接口生成一个 MCP 工具：几百个工具会挤满模型上下文，选错工具的概率高，部分客户端有数量限制。

### D8：修复关联提交

表 `sys_defect_commit`：`id`、`defect_id`、`defect_log_id`（对应那次修复日志）、`repository_id`（托管仓库时）、`commit_sha`、`commit_url`（外部仓库时）、`commit_message`、`create_by`、`create_time`。

- `repair_defect` 与网页的修复接口都可带提交列表。
- 托管仓库：校验提交存在于该项目的仓库，并且用户对该仓库有拉取权限；不存在时整个请求失败。
- 外部仓库：只校验 SHA 格式，记录链接。
- 缺陷详情的日志中展示关联提交；托管仓库的提交可以点开查看改动。

### D9：来源审计与写操作保护

- `sys_oper_log`、`sys_defect_log` 新增 `oper_channel`（`web` / `mcp` / `api`）与 `client_name`。网页日志与缺陷日志显示「经由 MCP（客户端名）」。
- `create_defect`、`create_case`、`comment_defect` 支持可选的 `idempotency_key`：同一用户、同一键在 24 小时内重复调用时，返回第一次的结果，不重复创建。
- MCP 调用按用户限流（例如每分钟 120 次，可配置）。

## Risks / Trade-offs

- [部分接口从数据库读用户的当前项目，而不是从会话读] → 通用通道转发时会作用到错误的项目。实施前审计项目内接口取项目 ID 的方式；有问题的接口要么改为从会话或参数取，要么从操作清单中排除，并写进测试。
- [回环转发的开销与部署差异（自定义端口、HTTPS、只监听特定地址）] → 回环地址与端口取自运行时实际监听配置；对通用通道单独做性能与超时测试。
- [AI 误操作造成数据破坏] → 工具注解让客户端提示确认；破坏性操作（删除）只出现在通用通道，不提供精做工具；所有操作有来源审计，可追溯。
- [项目设置默认全开导致新项目无意暴露] → 只对新项目生效，且权限仍受角色限制；在设置页说明含义。
- [MCP 协议版本演进] → 依赖 Quarkiverse 扩展跟进；接入文档写明已验证的客户端版本。

迁移：新增表和字段都有默认值，老数据不受影响；老项目默认关闭 MCP。回滚时关闭 `/mcp` 即可。

## Open Questions

- 精做工具的数量是否控制在 30 个左右？实施时看客户端的实际表现再调整。
- 是否需要 MCP 的资源（resources）或提示（prompts）能力，例如提供「项目缺陷规范」「提缺陷模板」？第一阶段先不做。

## 相关长期文档

- `docs/03-技术方案/权限模型与鉴权链路.md`（需新增「MCP 有效权限」一节）
- `docs/05-接口文档/接口约定.md`、`docs/05-接口文档/接口清单-OpenAPI与AI.md`
- `docs/02-系统架构/整体架构.md`（新增 MCP 入口）
- `docs/02-系统架构/数据库.md`（新增表与字段）
- `docs/03-技术方案/Git Smart HTTP与在线浏览.md`（提交校验与代码阅读复用）
- `docs/01-产品需求/产品定位与核心功能.md`（定位调整为代码质量管理 + AI）
- `docs/07-技术决策/ADR-008-系统管理员对Git仓库全权限.md`

拟新增 ADR：
- 「MCP 权限 = 角色权限 ∩ 项目 MCP 模块，系统级操作不开放」
- 「MCP 采用精做工具 + 通用通道，通用通道经回环转发复用网页接口」
