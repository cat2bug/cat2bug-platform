## ADDED Requirements

### Requirement: 编写用例引导读取本项目类型

`write_test_cases` 的引导消息 SHALL 要求 AI 先通过 `list_case_types` 或 `get_guide(topic=case)` 了解本项目类型，并在用户确认后再创建。类型 SHALL 被说明为可省略。引导 MUST NOT 要求在创建用例之前完成评审。

#### Scenario: 引导包含类型且不强制评审

- **WHEN** 客户端调用 `prompts/get`，name 为 `write_test_cases`
- **THEN** 返回的消息包含 `list_case_types` 或 `get_guide` 的 `case` 主题，写明类型可空，并且不把评审作为创建用例的前置步骤
