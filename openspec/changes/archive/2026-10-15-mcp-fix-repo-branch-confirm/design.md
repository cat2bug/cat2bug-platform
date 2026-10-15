## Context

修缺陷引导（`fix_defect` + `get_guide(topic=code)`）目前要求「拿不准就问仓库」，未区分单仓 / 多仓，也未约定基线分支何时默认可继续。用户反馈：单仓单分支场景不应反复确认；多仓或本地 remote 对不上时才问；问分支时要列出本地常见分支。

交付形态已由 [[ADR-017]] 定为「instructions 入口 + get_guide 正文 + Prompts 分步」；本次只补规则文案与轻量 hint，不引入新协议能力。

## Goals / Non-Goals

**Goals:**

- AI 在单仓且 remote 已对上时静默选用该仓与 `defaultBranch`，少打断用户。
- 多仓或 remote 失败时明确停问；问基线时始终附上本地常见分支列表。
- 规则出现在 instructions（一句）、`get_guide(topic=code)`（完整）、`fix_defect`（可执行步骤）、`get_current_project`（短 hint）。

**Non-Goals:**

- 不做服务端硬闸（不强制某工具顺序、不拦截 `repair_defect`）。
- 不解析用户本机 `git branch`（由客户端 AI 在本地执行后展示）。
- 不改多项目 OAuth、不自动选仓算法、不新增专用 confirm 工具。

## Decisions

### 1. 分层：instructions 一句 + guide 全文 + Prompt 步骤（采纳）

| 层 | 内容 |
|---|---|
| `instructions` | 一句：修代码前按 `get_guide(topic=code)`；单仓且 remote 对得上不要反复追问 |
| `get_guide(topic=code)` | 完整「仓库确认」「基线分支」规则与反例 |
| `fix_defect` Prompt | 与 code 主题一致的可勾选步骤 |
| `get_current_project` | 返回 `repositoryConfirmHint` 短句 + 工具 description 补一句（方案 B） |

备选：只改 Prompt → 非 `/fix_defect` 路径仍乱问。只加硬闸工具 → 超出「小变更」且违反「软规则」定案。

### 2. 仓库确认条件（精确）

1. 调用 `list_repositories` / `get_current_project` 得到本项目仓库列表。
2. 若本地有 git 工作区：对 `origin` 调 `resolve_project_by_git_remote`。
3. **不问仓库**：仓库列表恰好 1 个，且（无本地仓 **或** remote 解析到该唯一仓）。
4. **必须问**：仓库 ≥ 2，或 remote 解析失败 / 对不上列表中任一仓，或无法从缺陷描述判断多仓中的哪一个。
5. 问的时候列出平台仓库名（及 slug），请用户选一个。

### 3. 基线分支确认条件

1. 目标仓确定后，基线默认 = 该仓 `defaultBranch`（已由 list/get_current_project 返回）。
2. **每次**向用户说明或确认基线时，SHALL 附带本地常见分支列表（AI 在用户工作区执行 `git branch` / `git branch -a`，优先展示 `master`、`main`、`develop`、当前分支、以及与 `defaultBranch` 同名的本地分支）。
3. **可不追问、直接采用 defaultBranch**：本地存在与 `defaultBranch` 同名分支（或已检出该分支），且用户未提出异议。
4. **要问**：本地没有对应分支、存在多个合理候选（如同时有 `main` 与 `master` 且与 default 不符）、或用户工作区不在目标仓路径下。

### 4. `repositoryConfirmHint` 内容（固定中文短句）

例如：`单仓库且本地 origin 能 resolve 到该仓时不要追问仓库；基线默认用 defaultBranch，说明或确认基线时列出本地常见分支。多仓或 remote 对不上必须问用户。`

不按仓库数量动态改写（避免测试脆弱）；AI 仍以 guide 全文为准。

## Risks / Trade-offs

- [软规则依赖模型遵守] → Mitigation：三处正文一致 + 测试断言关键句；后续若仍频繁乱问再考虑硬闸。
- [instructions 被客户端截断] → Mitigation：完整规则在 `get_guide(topic=code)`，instructions 只做入口。
- [「常见分支」因工作区而异] → Mitigation：规范要求 AI 现场 `git branch`，不写死全局列表。

## Migration Plan

1. 改资源与配置、补 hint 字段与测试。
2. 已连接的 MCP 客户端需重连或重新 initialize 才能拿到新 instructions；guide/prompt 下次调用即生效。
3. 无数据迁移。

## 相关长期文档

- `docs/03-技术方案/MCP接入与权限.md`
- `docs/07-技术决策/ADR-017-MCP使用指南以工具加Prompt交付-正文放资源文件按项目实况拼接.md`
- `docs/07-技术决策/ADR-016-AI客户端只经OAuth登录接入且授权绑定单个项目.md`
