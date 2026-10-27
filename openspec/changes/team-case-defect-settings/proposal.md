## Why

用例类型、评审纪要模板、缺陷属性目前挂在**项目设置**，按 `projectId` 隔离；多项目团队要重复配置，口径易分叉。产品希望这三项成为**团队级统一配置**，仅团队创建人与团队管理员可改，项目侧只消费、不再维护。

## What Changes

- 将「用例类型」「评审纪要模板」「缺陷属性」的**配置入口**从项目设置迁到**团队设置**（新增卡片与子页）；项目设置中对应卡片与子路由入口 **BREAKING：直接删除**。
- 配置写 API 改为 **`/system/team/{teamId}/…`**；配置读在业务上下文（用例/缺陷）仍可带 `projectId`，服务端经 `project.team_id` 解析到团队配置。
- 权限改为仅 **`team.create-by` / `team.admin`** 可维护；项目管理员/项目创建人不再具备这三项的配置写权限（**BREAKING**）。
- 数据归属从 `project_id` 升为 `team_id`；升级迁移以**每团队最早创建的项目**为源拷贝到团队，并 remap 该团队下用例 `case_type` 等引用；无项目的团队按现有预置规则 seed。
- 一期覆盖三项：类型 + 模板 + 缺陷属性（含内置字段布局覆盖）。

## Capabilities

### New Capabilities

- `team-case-defect-settings`: 团队设置入口、team 级写 API、权限菜单、从项目迁出与升级迁移、项目设置卡片移除。
- `defect-field-catalog`: 缺陷属性（自定义字段 + 内置布局）在团队级的维护与项目内消费规则（首次成文；承接原项目级行为并改归属）。

### Modified Capabilities

- `case-type-catalog`: 名单归属由项目改为团队；维护角色改为团队创建人/管理员；用例绑定校验改为「类型属于用例所在项目所属团队」。
- `case-review`: 评审纪要模板归属团队；编辑壳权限改为团队侧 `case-type:edit` 等价权限（team 命名空间）。

## Impact

- 前端：`team/option` 增卡片与子页；删除 `CaseTypeCard` / `DefectSettingsCard` 及 `/project/case-types|case-settings|defect-fields` 入口；业务页 API 读路径适配。
- 后端：新 Team 资源；表结构 `sys_case_type` / 缺陷字段表 / 模板列迁到团队；Flyway 迁移与 remap；废弃或 410 旧 project 写接口。
- 权限：新 `system:team:case-type:*`、`system:team:defect-field:*`（或等价）赋给 role 12/4；收回项目侧对应菜单。
- 文档：`docs/01`、`05`、`02` 库表、`03` 模板与权限、`08` 版本记录；视情况补 ADR（配置归属团队）。
- 既有 OpenSpec：`case-type-catalog`、`case-review` 主 spec 行为变更；无推翻已有编号 ADR。
