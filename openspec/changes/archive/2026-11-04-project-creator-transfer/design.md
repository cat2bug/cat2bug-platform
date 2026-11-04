## Context

项目成员页已用 `projectCreateBy` 隐藏删除、锁定角色下拉，但无转让；`SysUserProjectService.deleteMember` 未拒绝创建人。团队侧已落地 `POST /system/team/{teamId}/transfer-creator`（仅创建人、受让人正式队员、角色迁移、会话权限刷新）。本变更在项目侧对齐，并额外同步 `sys_project.create_by`（探索已拍板）。

角色常量：`project.create-by` = 11，`project.admin` = 6。权限模型见长期文档。

## Goals / Non-Goals

**Goals:**

- 仅当前 `project.create-by` 本人可将创建人转给任意本项目成员。
- 事务内：迁移项目角色 + 更新 `sys_project.create_by`；始终一名创建人。
- 原创建人留在项目；无其它项目角色时落为 `project.admin`。
- UI 与团队成员转让一致（铭牌选人、确认、成功后刷新权限/列表）。
- 删除 API 拒绝持有 `project.create-by` 的成员。

**Non-Goals:**

- 不改缺陷/Git 删除交接流程（转让不触发 handoff）。
- 不要求受让人具备正式队籍。
- 不让项目管理员代为转让。
- 不改团队创建人转让。

## Decisions

1. **API 形态对齐团队**  
   `POST /system/project/{projectId}/transfer-creator`，body：`targetUserId`。不挂 `system:project:member:remove`；服务内校验调用者持有 `project.create-by`。  
   备选：复用 update-role —— 否决，无法原子换唯一创建人与 `create_by` 字段。

2. **受让人 = 任意项目成员**  
   `projectMembershipAuth.isProjectMember(target, projectId)`；含仅挂职用户。  
   备选：须正式队籍 —— 探索否决。

3. **角色迁移规则（对齐团队）**  
   受让人：清空其 `sys_user_project_role` 后只写 `project.create-by`。  
   原创建人：删除 `project.create-by`；若无剩余角色则插入 `project.admin`。  
   备选：受让人叠加 create-by —— 否决（前端创建人行只展示 create-by 选项，残留角色易乱）。

4. **同步 `sys_project.create_by`**  
   写为受让人 `userId`（与列表页 `project.createBy === currentUserId` 捷径一致）。  
   备选：只改角色 —— 探索否决。

5. **UI 可见性**  
   仅当「当前登录用户 == 该行创建人」时显示转让；他人看创建人行无删除、无转让。  
   候选列表：当前项目成员列表排除自己。

6. **会话权限**  
   转让成功后对当前用户调用 `updateRoleAndPermissionOfCurrentUser`；前端再 `getInfo` + 重拉成员列表（见踩坑记录）。

## Risks / Trade-offs

- [误转让] → 二次确认文案；转让后原创建人可被删，需知会产品。  
- [create_by 与角色短暂不一致] → 同事务更新。  
- [会话仍缓存 create-by] → 写接口刷新 + getInfo 重算。  
- [删除仍可绕过前端删创建人] → 服务端拒绝 create-by。

## Migration Plan

- 前后端同发；无 DB 迁移。  
- 回滚：隐藏转让按钮并保留旧删除行为即可；已转让数据以角色 + `create_by` 为准，无需回滚脚本。

## Open Questions

- 无（探索已拍板：一起改 create_by、任意项目成员、仅创建人本人）。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/03-技术方案/权限模型与鉴权链路.md`
- `docs/05-接口文档/接口清单-项目团队成员.md`
- `docs/06-踩坑记录/转让角色后会话权限未重算.md`
- `docs/06-踩坑记录/Select下拉嵌MemberNameplate须撑高option.md`
- `docs/08-版本记录/2026-11.md`
- `openspec/specs/team-creator-transfer/spec.md`（行为对照）
- 拟新增 ADR：无
