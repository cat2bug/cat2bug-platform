## 1. 后端：删除与转让

- [x] 1.1 `SysUserTeamService`（或等价）实现 `deleteTeamMember`：删角色 + `sys_user_team`；拒绝 `team.create-by`；不碰项目成员
- [x] 1.2 `TeamResource` 暴露 `DELETE /system/team/{teamId}/member/{memberId}`（权限与团队成员管理一致）
- [x] 1.3 实现 `transferCreator`：目标须正式队员；事务内迁移 `team.create-by`；原创建人降为普通团队角色
- [x] 1.4 `TeamResource` 暴露转让 API（如 `POST .../transfer-creator`）；仅当前创建人可调用
- [x] 1.5 确认 `max_members` 计数仅含有效 `sys_user_team`，删除后可再邀人

## 2. 后端：我的团队与正式队员标志

- [x] 2.1 改 `selectSysTeamListByUserId` / count：正式队籍 ∪ 该队下有 `sys_user_project` 的项目
- [x] 2.2 为当前团队上下文提供 `formalTeamMember`（或复用按 teamId 计算的 `teamMember`）供前端藏「全部项目」
- [x] 2.3 校验仅项目挂职用户：创建项目 / 团队邀请创建成员 / 团队设置写 API → 403

## 3. 前端：团队成员页

- [x] 3.1 移除锁定/解锁按钮与划线样式；非创建人行改为删除 + 确认文案（保留项目挂职提示）
- [x] 3.2 创建人行改为「转让」：选正式队员、确认、调转让 API
- [x] 3.3 i18n 文案（删除确认、转让标题/成功失败）

## 4. 前端：团队选择与项目列表

- [x] 4.1 `TeamSelect` / `myListTeam` 消费并集列表；仅项目挂职仍可切换到有挂职的团队
- [x] 4.2 `project/index.vue`：非正式队员隐藏「全部项目」Tab，默认「我参与的」
- [x] 4.3 隐藏/禁用仅项目挂职下的建项目、团队邀人入口（与后端 403 对齐）

## 5. 文档与验收

- [x] 5.1 更新 `docs/05-接口文档/接口清单-项目团队成员.md`（DELETE、transfer、my 并集语义）
- [x] 5.2 手工验收：删除保留项目权、创建人不可删、转让、离队后 Tab/选队、回队占配额、与项目删除独立（用户确认已测通）
- [x] 5.3 回写长期知识：接口清单已更新；无新 ADR；踩坑见会话缓存权限与 403 文案透传
