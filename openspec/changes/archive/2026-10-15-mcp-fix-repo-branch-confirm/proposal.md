## Why

AI 修缺陷时会反复追问「改哪个仓库、基于哪条分支」，即使项目只有一个仓库、本地 remote 已对上平台仓。需要把「何时必问 / 何时默认可继续」写成可执行规则，减少打断，又避免多仓或 remote 对不上时改错仓。

## What Changes

- 在 `get_guide(topic=code)` 与 `fix_defect` Prompt 中写明仓库 / 基线分支确认规则（软规则，不新增硬闸工具）：
  - **单仓库**且本地 `origin` 能用 `resolve_project_by_git_remote` 对上该仓 → **不问仓库**，直接用该仓；
  - **多仓库**，或本地 remote 对不上 / 无法解析 → **必须问用户选仓**；
  - 基线分支：默认用平台仓库的 `defaultBranch`；**每次要确认或说明基线时，列出本地常见分支**（如 `git branch` 里的 `master` / `main` / `develop` 等）供用户点选或纠正；仅在本地常见分支与 `defaultBranch` 不一致、或多候选时才追问。
- 初始化 `instructions` 增加一句入口提示：修代码前按 `get_guide(topic=code)` 确认仓库与基线；单仓且 remote 对得上时不要反复追问。
- `get_current_project`（B）：工具描述与返回中附简短 `repositoryConfirmHint`，提醒上述规则（仍靠文案，不做服务端拦截）。

## Capabilities

### New Capabilities

（无）

### Modified Capabilities

- `mcp-usage-guide`：`code` 主题与 instructions 增加仓库 / 基线确认规则
- `mcp-guided-prompts`：`fix_defect` 分步与上述规则对齐
- `mcp-tools`：`get_current_project` 增加确认 hint（描述 + 返回字段）

## Impact

- 资源：`mcp-guide/zh-CN/code.md`、`prompts/fix_defect.md`；可选 `overview.md` 一句交叉引用
- 配置：`application.properties` 的 `quarkus.mcp.server.server-info.instructions`
- 代码：`McpProjectTools.getCurrentProject`（hint 字段）；工具描述文案
- 测试：`QuarkusMcpServerTest` 断言 guide / prompt / instructions / `get_current_project` 含关键句
- 文档：`docs/03-技术方案/MCP接入与权限.md` 补一段确认规则；不新增 ADR（仍符合 ADR-017）
- **不**新增「必须先调用某工具才能 repair」的硬闸；**不**改 OAuth / 项目绑定
