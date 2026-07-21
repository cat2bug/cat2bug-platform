## 1. 数据库与领域模型

- [x] 1.1 运行 `./scripts/new-flyway-migration.sh member_invite_link` 生成三处 Flyway 占位
- [x] 1.2 编写 `sys_member_invite` 表 DDL（token、scope、team_id、project_id、role_ids JSON、expires_at、revoked、created_by、created_at）
- [x] 1.3 新增实体 `SysMemberInvite`、Mapper 接口与 XML（含按 token 查询、按团队/项目列表、撤销更新）

## 2. 邀请链接后端（阶段 1）

- [x] 2.1 实现 `SysMemberInviteService`：创建（固定 +1h）、预览、接受（团队/项目分支、幂等）、撤销
- [x] 2.2 团队邀请 REST：`POST/GET/DELETE /system/team/{teamId}/invite-link`（Spring + Quarkus 资源）
- [x] 2.3 项目邀请 REST：`POST/GET/DELETE /system/project/{projectId}/invite-link`（对称实现）
- [x] 2.4 公开接口：`GET /invite/{token}/preview`、`POST /invite/{token}/accept`（accept 需登录）
- [x] 2.5 权限注解与校验：仅团队/项目成员管理员可创建与撤销；token 熵与过期/撤销校验
- [x] 2.6 单元/集成测试：过期、撤销、多人接受、项目接受不写 `sys_user_team`

## 3. 邀请链接前端（阶段 1）

- [x] 3.1 新增 API 模块 `api/system/memberInvite.js`（或扩展现有 team/project API）
- [x] 3.2 新增路由与页面 `views/invite/index.vue`（预览、登录回跳、接受结果）
- [x] 3.3 `InviteTeamMember.vue` 增加「生成邀请链接」：选角色、展示 URL、复制、活跃链接列表与撤销
- [x] 3.4 项目成员管理（`AddProjectMember.vue` 或同级入口）增加对称邀请链接 UI
- [x] 3.5 i18n 文案：过期、已撤销、已是成员、接受成功

## 4. 项目成员解耦（阶段 2）

- [x] 4.1 修改 `SysUserProjectMapper.xml` 的 `selectNotSysUserListByProjectId`：全局用户减已入项目，去掉 `sys_user_team` 限制
- [x] 4.2 确认 `AddProjectMember.vue` 搜索与即时添加对新 SQL 生效
- [x] 4.3 扩展用户信息或 context API 返回 `teamMember`（当前团队是否在 `sys_user_team`）
- [x] 4.4 后端团队设置/成员变更 API 增加 membership 校验（非团队成员 403）
- [x] 4.5 测试：全局用户可被搜到并加入项目；仅项目成员无法调用团队写 API

## 5. 仅项目成员导航（阶段 2 — 方案 A）

- [x] 5.1 `TeamSelect.vue`：`teamMember=false` 时只读团队名、禁用团队切换
- [x] 5.2 侧栏/路由：过滤 `teamRouters` 与 `teamOptionRouters`（Vuex 或 layout 层）
- [x] 5.3 直链团队管理页面前端守卫（可选）与后端 403 双保险
- [x] 5.4 手工验证：仅项目成员可见项目菜单、可见团队名、不可进团队设置

## 6. 文档与收尾

- [x] 6.1 更新 `external.md` / 成员管理文档与实现一致
- [x] 6.2 运行 `mvn -Pquarkus -pl cat2bug-platform-quarkus-admin -am test` 相关模块
- [x] 6.3 更新 `openspec/changes/member-invite-and-project-access/TESTING.md` 手工测试清单（可选）
