## ADDED Requirements

### Requirement: get_current_project 附带仓库确认提示

`get_current_project` SHALL 在返回 JSON 中包含字符串字段 `repositoryConfirmHint`，用简短中文提示：单仓库且本地 origin 能 resolve 到该仓时不要追问仓库；基线默认用 `defaultBranch`，说明或确认基线时列出本地常见分支；多仓或 remote 对不上必须问用户。工具 `description` SHALL 含有指向该规则的简短说明（不必复述全文）。

#### Scenario: 返回含 repositoryConfirmHint
- **WHEN** 已授权用户调用 `get_current_project`
- **THEN** 响应 JSON 含非空字段 `repositoryConfirmHint`，且文案提到单仓库（或「不问」）与基线 / `defaultBranch` 之一
