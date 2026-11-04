## ADDED Requirements

### Requirement: 评审记录独立列表页

系统 SHALL 在项目命名空间提供评审记录列表页，路径为 `/{teamKey}/{projectKey}/case/reviews`（或等价 namespaced 子路径）。该页 MUST NOT 出现在项目侧栏菜单中；拥有用例查询权限的成员 SHALL 能打开并查看本项目评审批次。列表 MUST 使用项目列表页工具条与表格约定（左筛选、改值即查；`Cat2BugTable` 或等价业务表）。

#### Scenario: 从用例页进入列表

- **WHEN** 用户在用例列表工具栏下拉中选择「评审记录」
- **THEN** 浏览器进入本项目的 `case/reviews` 列表页

#### Scenario: 侧栏无独立菜单项

- **WHEN** 用户查看项目侧栏模块导航
- **THEN** 没有单独的「评审记录」一级菜单项

### Requirement: 列表可筛选并显示摘要

列表 API 与页面 SHALL 支持按状态（全部 / 进行中 `open` / 已完成 `completed`）、发起时间范围、以及可选发起人筛选，并分页。每行 MUST 展示足以浏览的字段，至少包含：评审标识、状态、发起人、发起时间、完成人、完成时间（进行中可空）、用例数、通过数、需修改数（及进行中待定数量若适用）。服务端 MUST 在列表响应中提供上述摘要统计，客户端 MUST NOT 为每一行再请求详情以拼统计。

#### Scenario: 按已完成筛选

- **WHEN** 用户将状态筛成「已完成」
- **THEN** 列表只显示 `status=completed` 的评审批次，且每行可见通过/需修改摘要

#### Scenario: 未传筛选项

- **WHEN** 客户端仅传 `projectId` 与分页参数请求列表
- **THEN** 返回该项目评审批次（可含 open 与 completed），每行仍含摘要字段

### Requirement: 评审详情独立页

系统 SHALL 提供详情页 `/{teamKey}/{projectKey}/case/reviews/{reviewId}`。详情 MUST 展示评审人（发起人；已完成时含完成人）、相关时间、统计（用例总数、通过、需修改，进行中含待定）、以及本批次用例列表（编号、标题、类型、结论、意见）。已完成时 MUST 展示落库的纪要正文（`report_markdown`）。进行中时 SHALL 提供「继续评审」入口，供有评审权限的成员完成该批次。无权限或不存在的评审 MUST NOT 当作本项目已授权内容展示。

#### Scenario: 查看已完成详情

- **WHEN** 用户在列表点击某条已完成评审的「查看」
- **THEN** 进入对应 `case/reviews/{reviewId}`，可见人、时间、统计、用例表与纪要

#### Scenario: 进行中可继续评审

- **WHEN** 有评审权限的用户打开一条 `open` 评审详情并选择继续评审
- **THEN** 进入与用例页一致的评审完成流程，可提交结论并完成该批次

### Requirement: 按落库纪要快照导出 md、doc、pdf

对 `completed` 且存在纪要正文的评审批次，拥有查看权限的成员 SHALL 能导出为 Markdown、Word（doc/docx）与 PDF。导出内容 MUST 为当前落库的 `report_markdown` 快照（含完成后用户改过的正文），MUST NOT 用团队当前纪要模板重新渲染后再导出。进行中（`open`）的评审 MUST NOT 提供成功导出；界面 MUST 禁用或拒绝并说明原因。导出为本地下载，MUST NOT 取代既有的「保存到文档」能力。

#### Scenario: 导出已完成纪要为 md

- **WHEN** 用户在已完成评审的列表或详情选择导出 Markdown
- **THEN** 浏览器下载的文件正文与该评审落库纪要一致

#### Scenario: 进行中不可导出

- **WHEN** 用户对一条 `open` 评审尝试导出任意格式
- **THEN** 导出不成功（控件禁用或明确错误），不产生纪要文件

#### Scenario: 改模板不影响已导出快照语义

- **WHEN** 团队之后修改了评审纪要模板，用户再导出更早完成的评审
- **THEN** 导出内容仍为该评审当时落库（及事后改稿）的纪要，而不是按新模板重生成
