## ADDED Requirements

### Requirement: Effective MCP modules intersect team module gate

项目 MCP 在启用且已勾选模块的前提下，有效模块集合 MUST 再与所属团队模块页面设置求交：团队关闭的开关键所映射的 MCP 模块键 MUST 不可用。团队 `case_plan` 关闭时，MCP 的 `case` 与 `plan` MUST 同时不可用。项目 MCP 设置界面 MUST 对团队已关闭的模块予以不可用或明确提示，且 MUST NOT 仅靠前端隐藏而允许服务端仍放行。

#### Scenario: Team-closed code blocks MCP despite project check

- **WHEN** 项目 MCP 已勾选 `code` 但所属团队关闭了 `code`
- **THEN** 该项目的 MCP 调用不能使用源码相关工具或操作

#### Scenario: Team case_plan off removes case and plan from MCP

- **WHEN** 团队关闭 `case_plan` 且项目 MCP 勾选了 `case` 与 `plan`
- **THEN** MCP 中用例与计划相关能力均不可用
