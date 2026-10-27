## Context

项目成员「搜索添加」UI 在 `AddProjectMember.vue`：远程 `el-select` 多选 + 角色多选；数据来自 `GET /system/project/{id}/not-member`。现行 Mapper 从项目所属团队的 `sys_user_team` 取候选，且 `notProjectMemberWhere` 只按 `nick_name` LIKE。OpenSpec 主规格 `project-member-without-team` 已要求全局 `sys_user`，实现未跟上。

直接添加 `POST .../member` 当前只读 `memberIds` 的 `Number`/`Long[]`，忽略 `roleIds`，且不走 `batchInsertSysUserProject`（邀请接受路径会写项目成员、默认缺陷 Tab、项目角色）。前端却提交整表单含 `roleIds`，导致「搜得到、加进去可能没角色」。

邀请链接 Tab（`MemberInviteLinkPanel`）已可把非团队的已注册用户拉进项目且不写团队；直接添加应对齐同一成员模型。下拉展示应对齐缺陷选人：`MemberNameplate`（名称下展示手机、邮箱）。

## Goals / Non-Goals

**Goals:**

- not-member 候选 = 全局已注册用户 − 已在本项目成员；可用昵称/手机/邮箱搜索。
- 下拉用 `MemberNameplate`；保持多选；邀请链接 Tab 不动。
- 直接添加写入项目成员 + 所选角色；不写入 `sys_user_team`。
- 修复 `memberIds` JSON 数组/`List` 解析，使前端现有提交可用。

**Non-Goals:**

- 未注册用户建号、按联系方式发邮件/短信邀请、allow-create 自由文本。
- 改邀请链接 TTL、多人共用、落地页流程。
- 团队「邀请成员」UI/搜索字段对齐（另开变更）。
- 结果「本团队优先」排序或标签（本期不做）。
- 新权限点或新表。

## Decisions

### 1. 候选池 SQL：仿团队 not-member，改为从 `sys_user` 排除项目成员

- **选择**：重写 `selectNotSysUserListByProjectId` / `countNotSysUserListByProjectId`：以 `sys_user` 为主表（`del_flag = '0'`），`LEFT JOIN sys_user_project`（未锁定）排除已在项目者；去掉对 `sys_user_team` 的依赖。
- **备选**：保留团队子查询再 UNION 全局 → 复杂且与 Spec「MUST NOT 要求团队成员」不符，否决。
- **搜索**：`search` 非空时 `(nick_name LIKE OR phonenumber LIKE OR email LIKE)`；空搜索返回空列表（前端已不发空搜）。

### 2. 直接添加走 `BatchUserRoleVo` + `batchInsertSysUserProject`

- **选择**：`ProjectResource.addMember` 反序列化为 `BatchUserRoleVo`（或等价解析 `memberIds`/`roleIds` 列表），调用已有 `batchInsertSysUserProject`；删除或降级为内部的无角色 `addMember(projectId, userId)` 不再作为 HTTP 主路径。
- **理由**：与邀请接受共用逻辑（成员行、默认「我的」缺陷 Tab、角色行）；天然不写团队。
- **备选**：仅在旧 `addMember` 上补角色循环 → 易漏 Tab/事务约定，否决。
- **解析**：兼容 Jackson 把数组解成 `List`/`Integer`/`Long`；`roleIds` 为空时返回现有业务错误（`project.member_role_not_empty`）。

### 3. 前端下拉复用 `MemberNameplate`，不新造展示组件

- **选择**：`el-option` 内用 `<member-nameplate :member="item" size="small" />`；placeholder/i18n 改为提示可按昵称、手机、邮箱搜索。
- **备选**：手写两行文字 → 与缺陷选人不一致，否决。
- **API**：继续 `listNotMemberOfProject` / `addProjectMembers`；若后端改为严格 `BatchUserRoleVo`，确认前端 body 字段名已对齐（`projectId`/`memberIds`/`roleIds`）。

### 4. 隐私与权限边界

- **选择**：具备 `system:project:member:add` 即可搜全局用户的昵称/手机/邮箱（与「能把人拉进项目」同权）；不新增脱敏字段。
- **备选**：仅返回手机/邮箱掩码 → 增加匹配困惑，本期不做。
- 响应仍含 `phoneNumber`/`email`（Mapper 已 select）；Nameplate 直接展示。

## Risks / Trade-offs

- [全局模糊搜可能扫大表] → 保持「必须输入关键词」+ 现有 limit/分页；若后续慢再加索引/前缀策略。
- [手机/邮箱空值用户只靠昵称命中] → 可接受；Nameplate 空联系方式按组件现有展示。
- [已有用户无角色被旧接口加入] → 本次强制 `roleIds`；前端已选角色必填校验应对齐（若尚未强制，补 form rule）。
- [重复添加已在项目用户] → SQL 已排除；并发双提交依赖 DB/业务唯一约束或二次校验（与现网一致）。

## Migration Plan

- 无 Flyway；纯查询与 API 行为修复。
- 发布后旧前端若只传单个 `memberId` 无角色会失败 → 当前 UI 已传 `memberIds`+`roleIds`，可接受。
- 回滚：还原 Mapper SQL 与 `ProjectResource.addMember`、前端 Nameplate/i18n。

## Open Questions

- 无（「本团队优先」明确不做；团队邀请对齐另开）。

## 相关长期文档

- `docs/05-接口文档/接口清单-项目团队成员.md`（not-member / 添加成员说明需随实现更新）
- `docs/01-产品需求/术语表.md`（团队 / 项目成员 / 邀请链接）
- `docs/03-技术方案/权限模型与鉴权链路.md`（项目成员与团队边界）
- `docs/02-系统架构/数据库.md`（`sys_user` / `sys_user_project` / `sys_user_project_role`，无 schema 变更）
- 拟新增 ADR：无
