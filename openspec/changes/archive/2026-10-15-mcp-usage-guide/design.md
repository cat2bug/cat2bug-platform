## Context

MCP 端点（`/mcp`）已有 35 个工具，授权绑定一个项目（ADR-016），有效权限为「角色权限 ∩ 项目开放模块」（ADR-014），工具经回环网关复用网页接口（ADR-015）。AI 目前能拿到的业务知识只有：

- `quarkus.mcp.server.server-info.instructions` 里四句话；
- 每个工具的一行描述（如 `repair_defect` 写了「要求你是当前处理人且状态为处理中或已驳回」）。

缺少的是「整体怎么用」：缺陷从提交到关闭的完整流转、每一步由谁做、项目里谁是测试谁是开发、测试计划与用例和缺陷的关系、报告和文档放在哪。用户希望 AI 能像加载了一个 Skill 一样，做完每一步就告诉用户下一步。

服务端框架 quarkus-mcp-server 1.9.1 已支持 `@Tool` 与 `@Prompt` / `@PromptArg`；Cursor 与 Claude Code 都支持 MCP Prompts（显示为 `/` 命令），对 MCP Resources 的支持不统一。

## Goals / Non-Goals

**Goals:**

- AI 调一次 `get_guide` 就能知道：本项目里我是什么角色、能用哪些模块、某个模块的业务流程与规则、每个状态下一步该谁做什么、该用哪个工具。
- 提供一组 Prompt，用户在客户端选一个就能被一步步引导完成提交缺陷、修复、审核、执行测试计划、写用例、看项目现状。
- 指南内容覆盖全部 MCP 模块：缺陷、用例、测试计划、交付物、报告、文档、代码（以及总览、角色）。
- 不放大权限：指南只描述，不执行；对用户无权的模块不展开步骤。

**Non-Goals:**

- 不做 MCP Resources（客户端支持不一致；以后需要再加，内容可复用）。
- 不做多语言：指南正文先只写简体中文，结构上预留按语言分目录。
- 不做网页端可编辑的指南（不入库、不加管理页面）。
- 不改任何工具的行为、权限、流转规则；只在少数流转工具描述末尾加一句「规则见 get_guide」。

## Decisions

### 1. 用工具 `get_guide` 交付指南，而不是 Resources 或只写 instructions

- instructions 只在初始化时发一次、客户端常截断或不展示给模型，放不下全部模块的说明，也无法按项目动态生成。
- Resources 需要 AI 主动「读资源」，Cursor 等客户端支持不稳定。
- 工具所有客户端都支持，且调用时能拿到当前连接的用户与项目，便于拼接项目实况。

instructions 只补一句「开始操作或回答业务问题前先调用 get_guide；每完成一步告诉用户结果与下一步」，作为入口。

### 2. 指南 = 静态正文（资源文件）+ 动态项目实况（代码生成）

- 静态正文放 `cat2bug-platform-admin/src/main/resources/mcp-guide/zh-CN/<topic>.md`，每个主题一个文件：`overview`、`roles`、`defect`、`case`、`plan`、`deliverable`、`report`、`document`、`code`。写业务概念、状态与流转、每一步的执行人、对应工具、常见错误与处理。改文案不必改 Java。
- 动态部分在 Java 里生成，附在正文前，作为「本项目实况」小节：
  - 项目名称与编号、本次授权的用户、他在项目中的角色名称；
  - 可用模块（复用 `get_current_project` 的 `usableModules` 计算），以及在当前主题模块里他具备的操作（如缺陷：能否创建 / 指派 / 修复 / 审核 / 关闭 / 重开，来自权限标识 `system:defect:*`）；
  - 主题为 `roles` 或 `overview` 时：项目成员按角色分组（每组最多 30 人，超出给数量）；
  - 主题为 `defect` 时：启用的内置字段与自定义字段名称（来自 `SysProjectDefectFieldService`），以及「指派给我、待我处理」的缺陷数量；
  - 主题为 `plan` 时：未完成的测试计划数量。
- 选资源文件而不是全部写在 Java 字符串里：正文较长（每主题约 1～3KB），Markdown 更好维护和审阅。资源目录要加入 `quarkus.native.resources.includes`（`application.properties` 与 `application-native.properties` 两处），否则 Native 镜像里读不到。

### 3. 按权限裁剪

- 主题对应的模块不在用户可用模块里：只返回实况小节和一句「你在本项目没有『xx』模块的权限（原因：角色未授予或项目未开放给 MCP），可请项目管理员在『AI 管理 → MCP 设置』中开启」，不返回操作步骤。
- `overview` 与 `roles` 任何已授权用户都可看。
- 正文里出现的工具名，若该工具当前用户不可用，不删除正文，但实况小节会列出「你可用的操作」，AI 以实况为准。正文开头写明这一点。

### 4. 「每一步之后告诉用户下一步」写进正文的状态表

缺陷主题正文包含状态表：每个状态的含义、谁是处理人、处理人可执行的动作与对应工具、执行后的新状态、应提示用户的下一步。例如：

| 状态 | 下一步谁做 | 可用动作（工具） | 之后 |
|---|---|---|---|
| 处理中 PROCESSING | 当前处理人（通常是开发） | 修复 `repair_defect`；转给别人 `assign_defect` | 待审核，处理人变为审核人 |
| 待审核 AUDIT | 审核人（通常是提交缺陷的测试） | 通过 `pass_defect`；驳回 `reject_defect` | 已关闭 / 已驳回 |
| 已驳回 REJECTED | 上一次的处理人 | 再次修复 `repair_defect` | 待审核 |
| 已关闭 CLOSED | 任何有权限的人 | 重新开启 `open_defect` | 处理中 |

规则以代码为准：处理人校验在 `McpDefectTools`（`requireHandler`、指派要求创建人或处理人），状态校验在 `SysDefectService.requireTransition`。正文需与之一致，测试中对状态表关键句做断言，防止规则改了文档没改。

### 5. Prompts：引导脚本，不带业务数据

用 `@Prompt` 定义（放在新类 `McpGuidePrompts`），每个 Prompt 返回一条 user 消息，内容是给 AI 的分步指令：先调用 `get_guide(topic=...)` 了解规则与自己的角色 → 用查询工具找到目标对象 → 向用户确认关键信息 → 执行操作 → 每步完成后用一两句话告诉用户结果与下一步、由谁来做。

| Prompt | 参数（均可选） | 用途 |
|---|---|---|
| `report_defect` | `summary` 问题简述 | 引导提交缺陷：补全标题、描述、复现步骤、优先级、交付物、处理人 |
| `fix_defect` | `defect_num` 缺陷编号 | 引导开发处理：读缺陷 → 定位代码 → 修复 → `repair_defect` 关联提交、指定审核人 |
| `review_defect` | `defect_num` | 引导审核：读修复说明与关联提交 → 验证 → 通过或驳回（驳回须写原因） |
| `run_test_plan` | `plan_name` 计划名称 | 引导执行测试计划：逐条执行子项、标记结果，不通过时创建缺陷并关联 |
| `write_test_cases` | `module_name` 交付物名称、`requirement` 需求描述 | 引导编写用例：选交付物 → 拟用例 → 用户确认 → 批量创建 |
| `project_status` | 无 | 汇总项目现状：我的待办、待审核、未完成计划、近期报告，并建议下一步 |

- Prompt 文本不读数据库、不做权限判断：它只是引导，真正的权限在工具里。这样 `prompts/list` 对所有已授权用户一致，实现简单。
- 每个 Prompt 都设置 `title`（避免 `"title": null`，见 `docs/06-踩坑记录/MCP工具注解缺title导致Cursor拒收整张工具表.md`）。
- Prompt 文本同样放资源文件 `mcp-guide/zh-CN/prompts/<name>.md`，参数用 `{{summary}}` 这类占位符替换；参数为空时替换为「（用户未提供，请先询问）」。

### 6. 代码位置

- `admin/mcp/McpGuideTools.java`：`get_guide` 工具（`readOnlyHint=true`，`title="Get usage guide"`）。
- `admin/mcp/McpGuidePrompts.java`：6 个 Prompt。
- `admin/mcp/McpGuideContent.java`：读取并缓存资源文件（启动后首次读取后缓存在内存）、替换占位符。
- 动态实况：项目、角色、可执行操作取自 `McpProjectAccess`；成员（`OpenApiMemberMapper`，需 `system:project:list` 或 `system:project:member`，否则只说明无权查看）与缺陷字段配置（`SysProjectDefectFieldService`）直接读；缺陷待办数量与测试计划进度经回环网关调用列表接口（与工具一致，受数据权限约束）。

## Risks / Trade-offs

- [正文与代码规则不一致，AI 按错误规则引导] → 正文只写规则，具体能否执行以工具结果为准；测试里断言缺陷状态表的关键句；在 `MCP接入与权限.md` 写明「改流转规则时同步改 `mcp-guide/zh-CN/defect.md`」。
- [指南过长占用 AI 上下文] → 按主题拆分，默认只返回 `overview`（约 2KB）；成员列表每组最多 30 人。
- [客户端不支持 Prompts] → 指南工具独立可用，Prompt 只是便捷入口。
- [Native 镜像漏打资源文件] → 加入 resources.includes，并在 Native 冒烟脚本中调用一次 `get_guide`。
- [非中文用户] → 先只提供中文；目录按 `zh-CN/` 预留，后续按 `Accept-Language` 或用户语言选择。

## Migration Plan

纯新增，无数据库改动。部署后 AI 客户端重新连接（或刷新工具列表）即可看到 `get_guide` 和 Prompts。回滚只需回退代码。

## Open Questions

- 指南是否需要随用户语言切换？本次先不做，等有非中文用户再定。
- 是否要把常用 Prompt 也提供给网页端的 AI 助手复用？本次不做。

## 拟新增 ADR

- ADR-017「MCP 使用指南以工具加 Prompt 交付，正文放资源文件，按项目实况动态拼接」。

## 相关长期文档

- `docs/03-技术方案/MCP接入与权限.md`
- `docs/03-技术方案/权限模型与鉴权链路.md`
- `docs/01-产品需求/术语表.md`
- `docs/01-产品需求/产品定位与核心功能.md`
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`
- `docs/06-踩坑记录/MCP工具注解缺title导致Cursor拒收整张工具表.md`
- ADR：`docs/07-技术决策/ADR-014-MCP权限为角色权限与项目模块的交集.md`、`ADR-015-MCP用精做工具加通用通道-经回环转发复用网页接口.md`、`ADR-016-AI客户端只经OAuth登录接入且授权绑定单个项目.md`
