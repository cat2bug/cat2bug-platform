## ADDED Requirements

### Requirement: fix_defect 遵循仓库与基线少打断规则

`fix_defect` Prompt 的分步指令 SHALL 要求 AI：

1. 用 `get_current_project` 或 `list_repositories` 取得仓库列表，并用 `resolve_project_by_git_remote`（有本地工作区时）核对 `origin`；
2. 单仓库且 remote 对得上（或无本地仓）时不要向用户追问仓库；
3. 多仓库或 remote 对不上时必须列出仓库请用户选择；
4. 基线默认用目标仓 `defaultBranch`；说明或确认基线时列出本地常见分支。

#### Scenario: fix_defect 含少打断步骤
- **WHEN** 调用 `prompts/get`，name 为 `fix_defect`
- **THEN** 返回消息包含 `resolve_project_by_git_remote`（或 `get_current_project`）、以及「不问」仓库或「单仓库」相关表述，并提到 `defaultBranch` 或「本地常见分支」
