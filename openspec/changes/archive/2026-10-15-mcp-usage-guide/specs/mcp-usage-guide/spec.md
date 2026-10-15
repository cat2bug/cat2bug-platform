## ADDED Requirements

### Requirement: 指南工具 get_guide

MCP 服务 SHALL 提供只读工具 `get_guide`，参数 `topic` 可选，取值 `overview`、`roles`、`defect`、`case`、`plan`、`deliverable`、`report`、`document`、`code`，缺省为 `overview`。工具 SHALL 声明 `title` 且 `readOnlyHint=true`。返回内容为 Markdown 文本，由「本项目实况」小节和该主题的业务说明组成。

#### Scenario: 缺省返回总览
- **WHEN** 已授权用户调用 `get_guide` 且不传 `topic`
- **THEN** 返回总览：项目名称、用户在项目中的角色、可用模块、各模块一句话说明、可选主题列表，以及「每完成一步告诉用户结果与下一步」的工作约定

#### Scenario: 非法主题
- **WHEN** 调用 `get_guide` 传入不在取值范围内的 `topic`
- **THEN** 工具返回错误，错误信息列出全部合法主题

#### Scenario: 工具列表中可见
- **WHEN** 客户端调用 `tools/list`
- **THEN** 列表中包含 `get_guide`，其 `annotations.title` 为非空字符串

### Requirement: 本项目实况

每个主题的返回 SHALL 以「本项目实况」小节开头，内容按当前连接的用户与授权项目实时生成，至少包含：项目名称与编号、用户昵称、用户在该项目的角色名称、可用模块列表、该主题模块下用户可执行的操作。

#### Scenario: 缺陷主题列出可执行操作
- **WHEN** 拥有缺陷创建与修复权限、但没有关闭权限的用户调用 `get_guide(topic=defect)`
- **THEN** 实况小节列出「创建」「修复」等可执行操作，不列出「关闭」

#### Scenario: 缺陷主题列出字段与待办
- **WHEN** 调用 `get_guide(topic=defect)`
- **THEN** 实况小节包含项目启用的缺陷内置字段与自定义字段名称，以及当前用户为处理人且未关闭的缺陷数量

#### Scenario: 角色主题列出成员
- **WHEN** 调用 `get_guide(topic=roles)`
- **THEN** 返回项目成员按角色分组的列表（每组最多 30 人，超出部分给出人数），并说明各角色在缺陷流程中通常承担的工作

#### Scenario: 无成员查看权限
- **WHEN** 用户没有 `system:project:list` 或 `system:project:member`（或项目未开放「项目」模块），调用 `get_guide(topic=roles)`
- **THEN** 实况小节写明「你无权查看本项目成员名单」，不列出成员，角色分工说明照常返回

### Requirement: 按权限裁剪主题

当 `topic` 对应的模块不在用户的可用模块中时，`get_guide` SHALL 只返回实况小节与无权说明，不返回该模块的操作步骤。`overview` 与 `roles` 对所有已授权用户可用。

#### Scenario: 无权模块
- **WHEN** 项目未向 MCP 开放「代码」模块，用户调用 `get_guide(topic=code)`
- **THEN** 返回实况小节和「你在本项目没有『代码』模块的权限」及开启方式说明，不包含代码模块的操作步骤

#### Scenario: 总览始终可用
- **WHEN** 任意已授权用户调用 `get_guide(topic=overview)` 或 `get_guide(topic=roles)`
- **THEN** 正常返回内容

### Requirement: 缺陷流转说明与代码规则一致

`defect` 主题 SHALL 包含缺陷状态表：处理中、待审核、已驳回、已关闭每个状态下由谁处理、可用动作与对应工具、执行后的新状态、应提示用户的下一步。内容 SHALL 与现有流转规则一致：修复要求当前处理人且状态为处理中或已驳回；审核通过与驳回要求当前处理人且状态为待审核；驳回退回上一次的处理人；指派要求创建人或当前处理人且未关闭；重开仅限已关闭。

#### Scenario: 状态表包含关键规则
- **WHEN** 调用 `get_guide(topic=defect)`
- **THEN** 返回内容包含 `repair_defect`、`pass_defect`、`reject_defect`、`assign_defect`、`open_defect` 与四个状态名称，并写明修复后进入待审核、驳回退回上一次的处理人

### Requirement: 全模块覆盖

`case`、`plan`、`deliverable`、`report`、`document`、`code` 主题 SHALL 各自说明：该模块的业务概念、与缺陷等其它模块的关系、常用操作的步骤与对应工具（专用工具或 `list_operations` / `call_operation` 通用通道）。

#### Scenario: 测试计划主题说明与缺陷的关系
- **WHEN** 有测试计划权限的用户调用 `get_guide(topic=plan)`
- **THEN** 返回内容说明测试计划子项的执行状态（pass / not_pass / unexecuted）、`update_plan_item` 的用法，以及不通过时创建缺陷并关联的步骤

### Requirement: 初始化说明引导调用指南

MCP 服务的初始化说明（instructions）SHALL 提示 AI：开始操作或回答业务问题前先调用 `get_guide`，并在每完成一步后告诉用户结果和下一步。

#### Scenario: initialize 返回的说明
- **WHEN** 客户端发送 `initialize`
- **THEN** 返回的 `instructions` 包含 `get_guide`

### Requirement: Native 镜像可读取指南正文

指南正文资源 SHALL 被打包进 JVM 包与 Native 镜像。

#### Scenario: Native 下调用指南
- **WHEN** 在 Native 可执行文件中调用 `get_guide(topic=defect)`
- **THEN** 返回完整正文，而不是「找不到指南」类错误
