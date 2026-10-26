## Why

项目「添加成员 → 搜索添加」候选池仍锁在同团队成员，且只能按昵称搜；与已归档能力 `project-member-without-team`（全局已注册用户）不一致，也无法用手机号/邮箱定位协作者。需要把直接添加对齐为：在全局用户中按昵称、手机、邮箱搜索并多选加入，且不把人拉进团队；邀请链接 Tab 继续保留。

## What Changes

- 将 `GET /system/project/{id}/not-member` 候选池改为**全局已注册用户**（排除已在该项目中者），不再要求先为同团队成员（落实既有 Spec，修正当前仍走 `sys_user_team` 的 SQL）。
- 搜索关键词同时匹配 **昵称、手机号、邮箱**（模糊）；空关键词不返回全库刷列表（保持现有「有输入才搜」行为）。
- 「搜索添加」下拉选项展示与缺陷选人一致：使用 `MemberNameplate`（头像 + 名称，下方手机与邮箱）；组件保持 **多选**。
- `POST /system/project/{id}/member` 正确解析 `memberIds`（含 JSON 数组/`List`），并写入所选 **角色**（`roleIds`）；加入后若用户此前无团队关系，**不**写入 `sys_user_team`。
- **不**支持未注册用户的自由文本建号/按联系方式发邀请；无匹配则无法选中提交。
- 「邀请链接」Tab 行为不变。
- 团队侧「邀请成员」下拉展示/搜索字段 **不在本次范围**（可另开变更）。

## Capabilities

### New Capabilities

（无。直接添加搜索与展示补强落在既有 `project-member-without-team` 上。）

### Modified Capabilities

- `project-member-without-team`：候选搜索扩展为昵称/手机/邮箱；下拉用 Nameplate 展示联系方式；直接添加须落库角色且不授予团队成员身份（与邀请链接路径一致）。

## Impact

- **后端**：`SysUserProjectMapper.xml`（`selectNotSysUserListByProjectId` / count 与 `notProjectMemberWhere`）；`ProjectResource.addMember`；`SysUserProjectService`（批量加人 + 角色，对齐邀请接受路径）。
- **前端**：`AddProjectMember.vue`（placeholder、下拉 `MemberNameplate`、必要时 i18n）；API 调用契约不变时可仍用现有 `listNotMemberOfProject` / `addProjectMembers`。
- **文档**：`docs/05-接口文档/接口清单-项目团队成员.md` 中 not-member 说明若写「团队非成员」需改为全局候选 + 搜索字段；不新增 ADR。
- **权限**：仍为 `system:project:member:add`；无新权限点。
- **非目标**：未注册拉新、团队邀请 UI 对齐、邀请链接 TTL/流程改动。
