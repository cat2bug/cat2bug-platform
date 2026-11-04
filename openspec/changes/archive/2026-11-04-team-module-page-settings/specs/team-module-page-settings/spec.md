## ADDED Requirements

### Requirement: Team stores module page settings

系统 MUST 按团队持久化功能模块页面设置，至少覆盖开关键 `case_plan`、`defect`、`deliverable`、`document`、`code`。每项 MUST 含是否显示（`enabled`）；`case_plan` MUST 另含用例与计划两个可选显示名；其余开关键 MUST 含一个可选显示名；`document` MUST 另含可选上传上限兆字节。缺省 MUST 为全部启用、显示名为空（使用产品默认文案）、文档上传上限为空（使用系统后台上限）。空配置或未落库 MUST 与缺省等价。

#### Scenario: Default all modules enabled

- **WHEN** 团队尚未保存过模块页面设置
- **THEN** 读接口返回全部模块启用且显示名为空

#### Scenario: Save deliverable alias

- **WHEN** 团队管理员将交付物显示名保存为「模块」并保持启用
- **THEN** 随后读取到该别名，且交付物模块仍为启用

### Requirement: Case and plan share one visibility switch

开关键 `case_plan` 的「显示模块」关闭时，系统 MUST 同时禁用用例与计划相关能力（菜单、路由、API、跨模块关联字段、工作台组件、MCP 中的 `case` 与 `plan`）。`case_plan` 的显示名 MUST 提供两个独立输入，分别覆盖用例与计划的可见文案。

#### Scenario: Disabling case_plan hides both

- **WHEN** 团队关闭 `case_plan` 且成员打开任意所属项目
- **THEN** 侧栏不出现用例与计划入口，且用例与计划业务 API 均被拒绝

#### Scenario: Two label fields

- **WHEN** 管理员将用例显示名设为「场景」、计划显示名设为「迭代」并保存
- **THEN** 前端与后端面向人的用例/计划文案分别使用这两个名称，而开关仍为同一个

### Requirement: Closed module is a hard gate for the whole team

当某开关键 `enabled` 为 false 时，该团队下所有项目 MUST 无法通过网页使用该模块（无论角色菜单权限），相关业务 API（含 Open API 与 MCP 回环）MUST 拒绝，跨模块表单/筛选中依赖该模块的属性 MUST 不展示。系统 MUST NOT 因关闭模块而删除既有业务数据。允许全部开关键同时关闭。

#### Scenario: Role cannot bypass closed module

- **WHEN** 团队关闭交付物且项目角色仍含交付物菜单权限
- **THEN** 成员侧栏无交付物入口，调用交付物 API 失败

#### Scenario: Closed case hides defect case fields

- **WHEN** 团队关闭 `case_plan` 且用户打开缺陷新建/编辑/筛选
- **THEN** 界面不展示测试用例相关属性或筛选

#### Scenario: Re-enable restores access without data loss

- **WHEN** 团队曾关闭文档后重新开启
- **THEN** 关闭期间未删除的文档仍可按权限访问

### Requirement: Display names override human-visible copy

非空显示名 MUST 覆盖该模块在前后端给人看的文案（含菜单、表单标签、提示、导出表头，以及 MCP 指南中的模块中文名）。URL 路径段、数据库标识、权限串、Open API 路径与字段名、MCP 模块键 MUST NOT 因显示名而改变。显示名为空时 MUST 回退产品默认 i18n。

#### Scenario: Alias appears in UI and export

- **WHEN** 交付物别名为「模块」且用户查看侧栏并导出含交付物列的表格
- **THEN** 侧栏与导出表头均显示「模块」而非默认「交付物」

#### Scenario: Identifiers unchanged

- **WHEN** 交付物已设置别名
- **THEN** 路由段仍为 `module`（或既有交付物段），权限前缀仍为 `system:module:`，MCP 键仍为 `deliverable`

### Requirement: Document upload max respects system ceiling

系统 MUST 提供可配置的全局上传文件最大尺寸（兆字节），缺省与现网业务上传上限一致。团队文档上传上限若配置，MUST 满足大于 0 且不超过当前全局上限，否则保存失败。文档实际上传（前端校验与后端校验）MUST 使用「团队配置值，若空则用全局上限」。

#### Scenario: Reject team max above system

- **WHEN** 全局上限为 500 且管理员提交文档 `maxUploadMb` 为 600
- **THEN** 保存失败且配置不变

#### Scenario: Upload blocked over team max

- **WHEN** 团队文档上限为 100MB 且用户上传大于 100MB 的文档
- **THEN** 上传被拒绝

### Requirement: Team hub cards and page-settings entry

团队设置枢纽 MUST 提供源码设置、用例与计划设置、交付物设置、文档设置卡片（用例与计划卡保留类型与评审模板入口），且各卡 MUST 提供「页面设置」导航。缺陷设置卡 MUST 继续提供其页面设置入口，并在其中维护缺陷开关键的显示开关与名称。仅具备相应团队配置权限的用户 SHALL 看到并进入这些入口。

#### Scenario: Hub shows new module cards

- **WHEN** 团队管理员打开团队设置枢纽
- **THEN** 可以看到源码、用例与计划、交付物、文档等设置卡片分区

#### Scenario: Open page settings from deliverable card

- **WHEN** 管理员从交付物设置卡进入页面设置并关闭显示模块
- **THEN** 该团队项目内交付物能力按硬闸门失效

### Requirement: Team and project APIs for module page settings

系统 MUST 提供 `GET/PUT /system/team/{teamId}/module-page-settings`（调用方须可管理该团队）以及项目只读 `GET /system/project/{projectId}/module-page-settings`，使项目成员无需打开团队设置也能取得所属团队配置并受其约束。

#### Scenario: Project member reads gate config

- **WHEN** 仅项目挂职用户进入项目
- **THEN** 其侧栏与业务请求仍遵循所属团队的模块页面设置
