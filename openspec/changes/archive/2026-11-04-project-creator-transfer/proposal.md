## Why

项目成员页已禁止删除/改角色持有 `project.create-by` 的人，但没有转让入口，创建人角色无法交接；后端删除也未拦创建人，只靠前端隐藏。团队侧已有「仅创建人本人转让」模式，项目应对齐，避免项目长期无人可移交所有权。

## What Changes

- 新增项目创建人转让：仅当前 `project.create-by` 本人可将该角色转给**任意本项目成员**；事务内同步更新 `sys_project.create_by`。
- 转让后原创建人保留项目成员身份；去掉 `project.create-by` 后若无其它项目角色则落到 `project.admin`；受让人角色改为仅 `project.create-by`。
- 创建人行 UI：删除改为「转让」（仅创建人本人可见）；弹框选成员（铭牌）+ 确认；成功后刷新会话权限与成员列表。
- 后端删除成员：持有 `project.create-by` 者不可删（与 UI 一致）；转让不触发缺陷/Git 交接。

## Capabilities

### New Capabilities

- `project-creator-transfer`: 项目创建人角色转让、始终一名创建人、更新 `sys_project.create_by`、仅创建人本人可操作、受让人为任意项目成员。

### Modified Capabilities

- `project-member-remove-handoff`: 明确持有 `project.create-by` 的成员不可通过删除/交接流程移除（须先转让）。

## Impact

- 后端：`SysUserProjectService`（或等价）转让事务；`ProjectResource` 暴露 `POST /system/project/{projectId}/transfer-creator`；`deleteMember` 拒绝创建人；转让后刷新当前用户权限。
- 前端：`project/member/index.vue`、`api/system/project.js`、i18n。
- 文档：`docs/05-接口文档/接口清单-项目团队成员.md`；无新 ADR（对齐已有团队转让与权限模型）。
