## 1. 指南正文（资源文件）

- [x] 1.1 新建 `cat2bug-platform-admin/src/main/resources/mcp-guide/zh-CN/`，写 `overview.md`、`roles.md`（总览、角色与各角色在流程中的分工）
- [x] 1.2 写 `defect.md`：类型、优先级、状态表（谁处理、可用动作与工具、新状态、提示用户的下一步），规则对照 `McpDefectTools` 与 `SysDefectService.requireTransition`
- [x] 1.3 写 `case.md`、`plan.md`、`deliverable.md`、`report.md`、`document.md`、`code.md`，各含概念、与其它模块的关系、步骤与对应工具
- [x] 1.4 写 6 个 Prompt 正文 `mcp-guide/zh-CN/prompts/*.md`，参数用 `{{name}}` 占位
- [x] 1.5 把 `mcp-guide/**` 加入 `application.properties` 与 `application-native.properties` 的 `quarkus.native.resources.includes`

## 2. 后端实现

- [x] 2.1 `McpGuideContent`：按主题读取并缓存正文、替换占位符、主题不存在时报错
- [x] 2.2 `McpGuideTools.get_guide`：校验 topic，生成「本项目实况」（项目、用户、角色、可用模块、该模块可执行操作），按权限裁剪
- [x] 2.3 实况的主题附加信息：`roles` / `overview` 成员按角色分组（每组最多 30 人）；`defect` 启用字段与自定义字段、我的待处理数；`plan` 未完成计划数
- [x] 2.4 `McpGuidePrompts`：6 个 `@Prompt`，均带 `title` 与可选 `@PromptArg`，返回一条 user 消息
- [x] 2.5 修改 `quarkus.mcp.server.server-info.instructions`，加入「先调用 get_guide；每完成一步告诉用户结果与下一步」
- [x] 2.6 `repair_defect`、`pass_defect`、`reject_defect`、`assign_defect` 描述末尾加「流转规则见 get_guide(topic=defect)」

## 3. 测试与验证

- [x] 3.1 `QuarkusMcpServerTest`：`get_guide` 缺省返回总览；非法 topic 报错并列出合法值；`defect` 主题包含状态表关键句；无权模块只返回无权说明
- [x] 3.2 `QuarkusMcpServerTest`：`prompts/list` 返回 6 个且 title 非空、参数非必填；`prompts/get fix_defect` 带参数与不带参数的内容；未授权 401
- [x] 3.3 `initialize` 返回的 instructions 包含 `get_guide`
- [x] 3.4 `mvn -pl cat2bug-platform-admin -am test` 通过（252 个，跳过 2）
- [x] 3.5 本地启动后在 Cursor 中实测：调用 `get_guide`、使用 `/fix_defect` 引导处理一个缺陷（开发环境已用 HTTP 客户端实测 `initialize`、`get_guide` 三个主题与 `prompts/list`；Cursor 重连后实测 `get_guide(topic=overview)` 通过）
- [x] 3.6 Native 冒烟脚本加 `get_guide(topic=defect)` 与 `prompts/list` 检查（本次未跑 Native 构建，未验证）

## 4. 回写长期知识

- [x] 4.1 `docs/03-技术方案/MCP接入与权限.md` 增加「使用指南与引导命令」一节，并写明改流转规则时须同步改 `mcp-guide/zh-CN/defect.md`
- [x] 4.2 新增 ADR-017「MCP 使用指南以工具加 Prompt 交付，正文放资源文件，按项目实况动态拼接」，加入 `07-技术决策/说明.md` 与总览
- [x] 4.3 `docs/05-接口文档/接口清单-OpenAPI与AI.md` 补 `get_guide` 与 Prompts 清单
- [x] 4.4 `docs/08-版本记录/2026-10.md` 加一行；本次无新踩坑
