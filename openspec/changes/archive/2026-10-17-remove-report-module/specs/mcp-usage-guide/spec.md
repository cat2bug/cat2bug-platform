## MODIFIED Requirements

### Requirement: 指南工具 get_guide

MCP 服务 SHALL 提供只读工具 `get_guide`，参数 `topic` 可选，取值 `overview`、`roles`、`defect`、`case`、`plan`、`deliverable`、`document`、`code`，缺省为 `overview`。工具 SHALL 声明 `title` 且 `readOnlyHint=true`。返回内容为 Markdown 文本，由「本项目实况」小节和该主题的业务说明组成。`report` MUST NOT 作为合法主题。

#### Scenario: 缺省返回总览
- **WHEN** 已授权用户调用 `get_guide` 且不传 `topic`
- **THEN** 返回总览：项目名称、用户在项目中的角色、可用模块、各模块一句话说明、可选主题列表，以及「每完成一步告诉用户结果与下一步」的工作约定

#### Scenario: 非法主题
- **WHEN** 调用 `get_guide` 传入不在取值范围内的 `topic`（含 `report`）
- **THEN** 工具返回错误，错误信息列出全部合法主题，且该列表不含 `report`

#### Scenario: 工具列表中可见
- **WHEN** 客户端调用 `tools/list`
- **THEN** 列表中包含 `get_guide`，其 `annotations.title` 为非空字符串

### Requirement: 全模块覆盖

`case`、`plan`、`deliverable`、`document`、`code` 主题 SHALL 各自说明：该模块的业务概念、与缺陷等其它模块的关系、常用操作的步骤与对应工具（专用工具或 `list_operations` / `call_operation` 通用通道）。

#### Scenario: 测试计划主题说明与缺陷的关系
- **WHEN** 有测试计划权限的用户调用 `get_guide(topic=plan)`
- **THEN** 返回内容说明测试计划子项的执行状态（pass / not_pass / unexecuted）、`update_plan_item` 的用法，以及不通过时创建缺陷并关联的步骤
