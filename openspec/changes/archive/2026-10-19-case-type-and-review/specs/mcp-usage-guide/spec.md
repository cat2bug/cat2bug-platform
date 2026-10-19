## ADDED Requirements

### Requirement: 用例主题说明类型与可选评审

`case` 主题 SHALL 说明：用例类型来自本项目名单且可空；评审是对勾选用例的一次活动，不是用例的执行状态；测试计划中的评审条件只用于筛选，不阻止加入计划。`get_guide(topic=case)` 的本项目实况 SHALL 列出该项目当前的类型名称。主题 MUST NOT 把评审纪要描述为已删除的报告模块。

#### Scenario: 实况列出类型

- **WHEN** 有用例权限的用户调用 `get_guide(topic=case)`，项目类型为「功能」和「接口」
- **THEN** 实况小节包含「功能」和「接口」，正文说明类型可空，并说明评审不阻断计划

#### Scenario: 不提及报告模块

- **WHEN** 用户调用 `get_guide(topic=case)`
- **THEN** 返回内容不指引用户使用 `list_reports`、`get_report` 或 `/system/report`
