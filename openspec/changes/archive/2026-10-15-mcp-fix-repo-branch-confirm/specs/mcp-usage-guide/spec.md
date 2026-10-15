## ADDED Requirements

### Requirement: 代码主题的仓库与基线确认规则

`get_guide(topic=code)` 的正文 SHALL 包含「仓库确认」与「基线分支」规则，且与下列行为一致：

1. **不问仓库**：本项目恰好一个源码仓库，且（客户端无本地 git 工作区，或本地 `origin` 经 `resolve_project_by_git_remote` 解析到该唯一仓库）时，AI MUST NOT 向用户追问改哪个仓库，直接使用该仓库。
2. **必须问仓库**：本项目有两个及以上源码仓库，或本地 remote 无法解析 / 对不上本项目任一仓库时，AI MUST 停下并向用户列出平台仓库请其选择。
3. **基线分支**：默认采用目标仓库的 `defaultBranch`；凡向用户说明或确认基线分支时，MUST 附带本地常见分支列表（由 AI 在用户工作区列出，至少覆盖本地已有的 `master` / `main` / `develop`、当前分支，以及与 `defaultBranch` 同名的分支）。仅当本地缺少对应分支、存在与 default 不符的多个候选、或工作区不在目标仓时，才追问用户选哪条基线。

#### Scenario: code 主题包含确认规则关键词
- **WHEN** 有代码模块权限的用户调用 `get_guide(topic=code)`
- **THEN** 返回正文包含「不问仓库」或等价表述、`resolve_project_by_git_remote`、`defaultBranch`，以及「本地常见分支」相关说明

### Requirement: 初始化说明提示少打断

MCP 服务的初始化说明（instructions）SHALL 提示：修代码前按 `get_guide(topic=code)` 确认仓库与基线；单仓库且本地 remote 对得上时不要反复追问。

#### Scenario: initialize 含少打断提示
- **WHEN** 客户端发送 `initialize`
- **THEN** 返回的 `instructions` 同时包含 `get_guide` 与「不要反复追问」（或等价措辞，如「单仓」与「不要」同时出现）
