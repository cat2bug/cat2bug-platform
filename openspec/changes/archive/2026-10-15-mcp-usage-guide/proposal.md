## Why

AI 客户端接入 MCP 后只拿到四句话的初始化说明（instructions）和每个工具的一行描述，不知道 cat2bug 的业务规则：缺陷有哪些状态、谁能在哪一步做什么、项目里有哪些角色、测试计划怎么推进。结果是 AI 只会机械调用工具，用户问「接下来该做什么」时答不上来，也容易在流转上犯错（比如让不是处理人的人去提交修复）。需要像 Skill 一样，把「系统怎么用」交给 AI，让它每做完一步都能告诉用户下一步。

## What Changes

- 新增指南工具 `get_guide`：按主题返回使用指南（总览、缺陷、用例、测试计划、交付物、报告、文档、代码），内容由「固定的业务说明」加「本次授权项目的实际情况」拼成：当前用户在项目里的角色和可用模块、项目各角色有哪些成员、缺陷启用了哪些字段与自定义字段等。用户没有权限的模块，只说明「你没有该模块权限」，不展开操作步骤。
- 新增 MCP Prompts（客户端里显示为可选的引导命令）：提交缺陷、处理 / 修复缺陷、审核缺陷、执行测试计划、编写测试用例、项目现状概览。每个 Prompt 生成一段带步骤的引导，让 AI 先读指南、再一步步调用工具，并在每步完成后告诉用户结果和下一步。
- 初始化说明（instructions）补一句：开始操作或回答业务问题前，先调用 `get_guide`；每完成一步告诉用户结果和下一步。
- 工具描述中涉及流转的（`repair_defect`、`pass_defect`、`reject_defect` 等）在描述末尾加「流转规则见 get_guide(topic=defect)」。
- 文档：`docs/03-技术方案/MCP接入与权限.md` 增加「使用指南与引导命令」一节。

## Capabilities

### New Capabilities

- `mcp-usage-guide`：指南工具 `get_guide` 的主题、内容组成（业务说明 + 项目实况）、按权限裁剪规则，以及初始化说明对它的引用。
- `mcp-guided-prompts`：MCP Prompts 的清单、参数与生成内容要求（引导 AI 逐步操作并在每步后告知用户）。

### Modified Capabilities

（无。`mcp-endpoint`、`mcp-tools` 仍在未归档的变更 `mcp-server`、`mcp-oauth-project-binding` 中，本变更不改它们的已有要求，只新增能力。）

## Impact

- 后端：`cat2bug-platform-admin/.../admin/mcp/` 新增指南工具类与 Prompt 类；指南正文放在 `src/main/resources/mcp-guide/`（Markdown），需加入 `quarkus.native.resources.includes`。
- 配置：`application.properties` 的 `quarkus.mcp.server.server-info.instructions`。
- 测试：`QuarkusMcpServerTest` 增加指南与 Prompt 用例；Native 冒烟可选加一次 `get_guide` 调用。
- 不改数据库、不改网页接口、不改权限规则（遵守 ADR-014、ADR-015、ADR-016）。
- 客户端兼容：工具所有客户端都支持；Prompts 在 Cursor、Claude Code 中可用，不支持 Prompts 的客户端不受影响。
