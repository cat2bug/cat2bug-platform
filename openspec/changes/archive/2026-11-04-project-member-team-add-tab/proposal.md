## Why

项目「添加成员」弹框目前只有「搜索添加」（全局用户、必须有关键词）和「邀请链接」。从本团队批量挑选尚未入项的人需要先搜索，效率低。需要单独的「团队添加」入口：列出有界的团队候选并勾选加入。

## What Changes

- 添加成员弹框 Tab 最前增加「团队添加」，打开弹框时默认落在该 Tab
- 该 Tab：上方选择项目角色（多选，口径同搜索添加），下方勾选「本项目所属团队成员 ∩ 尚未加入本项目」的用户；支持轻量搜索与分页
- 新增专用列表接口（不扩展现有 `not-member`）：允许空关键词返回团队未入项成员
- 提交仍走现有批量添加项目成员接口（只写项目成员与角色，不改团队关系）
- 「搜索添加」「邀请链接」保留且行为不变（空搜禁刷全库等 `project-member-without-team` 约束不变）

## Capabilities

### New Capabilities

- `project-member-team-add-tab`: 项目添加成员弹框「团队添加」Tab、专用团队未入项成员列表 API 与勾选提交流程

### Modified Capabilities

- （无）不修改 `project-member-without-team` / `member-invite-link` 的既有要求；本变更为并行入口

## Impact

- 前端：`AddProjectMember.vue`、项目成员 API 客户端、i18n
- 后端：`ProjectResource` + `SysUserProjectService` / Mapper 新增按项目 `team_id` 过滤的未入项成员查询
- 文档：接口清单「项目团队成员」补一行；不推翻既有 ADR
