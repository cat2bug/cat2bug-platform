## 1. Backend：候选搜索

- [x] 1.1 重写 `SysUserProjectMapper.xml` 的 `selectNotSysUserListByProjectId` / `countNotSysUserListByProjectId`：从全局 `sys_user` 排除已在项目成员，去掉对 `sys_user_team` 的依赖
- [x] 1.2 更新 `notProjectMemberWhere`（或等价条件）：非空 `search` 时按 `nick_name` / `phonenumber` / `email` 模糊匹配；空搜索不返回全库

## 2. Backend：直接添加

- [x] 2.1 将 `ProjectResource.addMember` 改为接受 `BatchUserRoleVo`（或等价解析 `memberIds`/`roleIds` 的 List/数组），调用 `batchInsertSysUserProject`
- [x] 2.2 确认直接添加路径不写入 `sys_user_team`；无角色时返回既有「角色不能为空」类错误
- [x] 2.3 必要时清理或收窄仅插入成员行、不写角色的 `addMember(projectId, userId)` HTTP 用法，避免回归无角色加入

## 3. Frontend：添加成员弹窗

- [x] 3.1 `AddProjectMember.vue` 下拉选项改为 `MemberNameplate`（`size="small"`），保持多选 remote select
- [x] 3.2 更新搜索 placeholder / i18n，提示可按昵称、手机号、邮箱搜索
- [x] 3.3 表单校验强制至少选一个角色、至少一个成员；提交仍走 `addProjectMembers`，确认 body 与后端契约一致

## 4. 验证与文档

- [x] 4.1 手工验证：按昵称/手机/邮箱搜到非团队用户；多选加入后有角色且无团队关系；已在项目用户不出现；邀请链接 Tab 仍可用
- [x] 4.2 更新 `docs/05-接口文档/接口清单-项目团队成员.md` 中 not-member / 添加成员说明（全局候选 + 搜索字段 + 角色）

## 5. 回写长期知识

- [x] 5.1 无新 ADR、无单独踩坑篇；接口清单已在 4.2 更新；术语表/权限模型文档事实未变，无需改动

## 6. 跟进：下拉加大与团队标记

- [x] 6.1 弹框加宽、下拉项加高；not-member 返回 `teamMember`；头像右下角「团」标记

