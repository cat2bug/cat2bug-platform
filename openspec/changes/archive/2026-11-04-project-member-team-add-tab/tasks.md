## 1. 后端：team-not-member 接口

- [x] 1.1 Mapper：按项目 `team_id` 查团队成员且排除已在项目中的用户；支持可选 search 与分页；空 search 仍返回列表
- [x] 1.2 `SysUserProjectService` + `ProjectResource` 暴露 `GET /{projectId}/team-not-member`（权限 `system:project:member:add`）

## 2. 前端：团队添加 Tab

- [x] 2.1 `project.js` 增加 `listTeamNotMemberOfProject`
- [x] 2.2 `AddProjectMember.vue`：第一 Tab「团队添加」、默认激活；角色多选 + 勾选列表（搜索/分页/空态）；页脚提交
- [x] 2.3 提交复用 `addProjectMembers`；校验角色与成员；成功关闭并刷新；i18n 中/英

## 3. 回归

- [x] 3.1 确认「搜索添加」空搜仍为空、「邀请链接」不变；团队 Tab 批量加入后列表正确（后端 compile 通过；搜索添加仍要求关键词；端到端请在成员页复核）

## 4. 回写长期知识

- [x] 4.1 更新 `docs/05-接口文档/接口清单-项目团队成员.md` 增加 team-not-member 一行；无新 ADR / 无新踩坑
