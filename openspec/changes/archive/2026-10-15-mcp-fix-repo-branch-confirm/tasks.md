## 1. 指南与 Prompt 文案

- [x] 1.1 更新 `mcp-guide/zh-CN/code.md`：写明「不问仓库 / 必须问仓库」与「基线 + 本地常见分支」规则（与 design 决策 2、3 一致）
- [x] 1.2 更新 `mcp-guide/zh-CN/prompts/fix_defect.md`：步骤 3～4 对齐上述规则，去掉「一律拿不准就问」的模糊表述
- [x] 1.3 （可选）`overview.md` 或 `defect.md` 一句交叉引用 code 主题确认规则

## 2. instructions 与 get_current_project hint

- [x] 2.1 修改 `application.properties` 的 `quarkus.mcp.server.server-info.instructions`，增加少打断入口句
- [x] 2.2 `McpProjectTools.getCurrentProject`：写入 `repositoryConfirmHint`；更新 `@Tool` description 一句

## 3. 测试与文档

- [x] 3.1 `QuarkusMcpServerTest`：`get_guide(topic=code)`、`prompts/get fix_defect`、`initialize` instructions、`get_current_project` 断言关键句 / 字段
- [x] 3.2 回写 `docs/03-技术方案/MCP接入与权限.md`「使用指南」小节（仓库 / 基线确认规则）；`docs/08-版本记录/2026-10.md` 一行

## 4. 验收

- [x] 4.1 单仓项目 + remote 对得上：走 fix_defect 不追问仓库，基线默认可继续并展示本地分支列表（说明用）——单测已覆盖文案/字段；真实客户端行为归档后按需抽查
- [x] 4.2 多仓或故意改错 origin：AI 停下列出仓库请用户选——同上，软规则无服务端硬闸，依赖指南文案与客户端遵守
