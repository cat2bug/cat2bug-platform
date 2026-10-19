# mcp-guided-prompts Specification

## Requirements

### Requirement: 引导命令清单

MCP 服务 SHALL 通过 MCP Prompts 提供以下引导命令，每个都声明非空 `title` 与 `description`，参数均为可选：

- `report_defect`（参数 `summary`）：提交缺陷
- `fix_defect`（参数 `defect_num`）：处理 / 修复缺陷
- `review_defect`（参数 `defect_num`）：审核缺陷
- `run_test_plan`（参数 `plan_name`）：执行测试计划
- `write_test_cases`（参数 `module_name`、`requirement`）：编写测试用例
- `project_status`（无参数）：项目现状概览

#### Scenario: 列出引导命令
- **WHEN** 已授权客户端调用 `prompts/list`
- **THEN** 返回上述 6 个 Prompt，每个的 `title` 为非空字符串，参数均为非必填

#### Scenario: 未授权不可用
- **WHEN** 未携带有效 OAuth 令牌的请求调用 `prompts/list`
- **THEN** 与工具一致返回 401

### Requirement: 引导内容的结构

每个 Prompt 的 `prompts/get` 结果 SHALL 返回一条 user 消息，内容为给 AI 的分步指令，并 SHALL 包含：

1. 先调用 `get_guide` 的对应主题了解规则和自己的角色；
2. 用查询工具定位目标对象，信息不全时先向用户询问；
3. 写操作前向用户确认关键内容；
4. 每完成一步，用简短的话告诉用户结果、下一步做什么、由谁来做。

#### Scenario: 修复缺陷引导
- **WHEN** 调用 `prompts/get`，name 为 `fix_defect`，参数 `defect_num=71`
- **THEN** 返回的消息包含缺陷编号 71、`get_guide`、`get_defect`、`repair_defect`，并要求修复后提示用户缺陷待审核、由审核人验证

#### Scenario: 参数未提供
- **WHEN** 调用 `prompts/get`，name 为 `fix_defect`，不传 `defect_num`
- **THEN** 返回的消息要求 AI 先查询指派给当前用户的缺陷，并请用户选择要处理哪一个

### Requirement: 引导命令不放大权限

Prompt SHALL 只生成文字，不执行任何读写；实际操作仍经工具完成并受「角色权限 ∩ 项目开放模块」约束。

#### Scenario: 无权用户使用引导
- **WHEN** 没有测试计划权限的用户使用 `run_test_plan`
- **THEN** Prompt 正常返回引导文字；AI 按引导调用 `get_guide(topic=plan)` 时得到无权说明，调用计划工具时被拒绝

### Requirement: fix_defect 遵循仓库与基线少打断规则

`fix_defect` Prompt 的分步指令 SHALL 要求 AI：

1. 用 `get_current_project` 或 `list_repositories` 取得仓库列表，并用 `resolve_project_by_git_remote`（有本地工作区时）核对 `origin`；
2. 单仓库且 remote 对得上（或无本地仓）时不要向用户追问仓库；
3. 多仓库或 remote 对不上时必须列出仓库请用户选择；
4. 基线默认用目标仓 `defaultBranch`；说明或确认基线时列出本地常见分支。

#### Scenario: fix_defect 含少打断步骤
- **WHEN** 调用 `prompts/get`，name 为 `fix_defect`
- **THEN** 返回消息包含 `resolve_project_by_git_remote`（或 `get_current_project`）、以及「不问」仓库或「单仓库」相关表述，并提到 `defaultBranch` 或「本地常见分支」

### Requirement: 编写用例引导读取本项目类型

`write_test_cases` 的引导消息 SHALL 要求 AI 先通过 `list_case_types` 或 `get_guide(topic=case)` 了解本项目类型，并在用户确认后再创建。类型 SHALL 被说明为可省略。引导 MUST NOT 要求在创建用例之前完成评审。

#### Scenario: 引导包含类型且不强制评审

- **WHEN** 客户端调用 `prompts/get`，name 为 `write_test_cases`
- **THEN** 返回的消息包含 `list_case_types` 或 `get_guide` 的 `case` 主题，写明类型可空，并且不把评审作为创建用例的前置步骤
