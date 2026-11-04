## Context

`AddProjectMember.vue` 现有 Tab：`direct`（搜索添加，远程搜 `GET .../not-member`，空关键词强制空列表）与 `link`（邀请链接）。搜索结果虽按 `teamMember` 分组，但不能空搜列出本团队未入项成员。`project-member-without-team` 要求全局候选须关键词，不得刷全库——因此团队勾选不能复用同一空搜规则。

探索结论：新建专用接口（方案 B），弹框第一 Tab「团队添加」，默认打开；角色上、勾选下；提交复用 `POST .../member`。

## Goals / Non-Goals

**Goals:**

- 专用列表 API：本项目 `team_id` 下、未加入本项目、未锁团队成员；允许空搜分页；可选昵称/手机/邮箱模糊
- UI：第一 Tab「团队添加」；角色多选 + 勾选列表；提交添加项目成员
- 不破坏搜索添加 / 邀请链接既有行为

**Non-Goals:**

- 不把人选进团队（候选已是团队成员）
- 不改邀请链接、不改 `not-member` 空搜禁令
- 不做每人不同角色（一批人共用上方 `roleIds`）
- 无新 ADR

## Decisions

### D1. 专用接口而非扩展 `not-member`

- **选择**：`GET /system/project/{projectId}/team-not-member`（权限同添加成员：`system:project:member:add`）
- **理由**：语义分离——有界团队池可空搜；全局池不可。避免前端用开关误触全库。
- **备选否决**：`not-member?teamOnly=true`——易与空搜规则纠缠，拒绝。

### D2. 查询口径

- 项目 → `sys_project.team_id`
- 候选：`sys_user_team`（同 `team_id`，`team_lock != 1`）且无有效 `sys_user_project`（同 `project_id`，`project_lock != 1`）
- 排除停用/删除用户（与现有 not-member / 团队成员列表一致）
- `search` 可选：空则全量分页；非空则昵称/手机/邮箱模糊（同 not-member）
- 分页：`pageNum` / `pageSize`，默认与前端约定（如 20）

### D3. UI 与提交

- Tab 顺序：`team` → `direct` → `link`；`open()` 设 `activeTab = 'team'`
- 团队 Tab：`el-select` 角色 + 可搜索输入框 + checkbox 列表（`MemberNameplate`）；全选当前页可选
- 页脚：`team` / `direct` 显示提交；`link` 仍无面板内操作
- 提交：`addProjectMembers(projectId, { memberIds, roleIds })`，与搜索添加同一校验（角色、成员非空）
- 仅项目成员操作者：仍展示 Tab（按项目 `team_id` 查）；空列表空态文案

### D4. 与 `project-member-without-team` 边界

- `not-member` / 搜索添加：不变
- 团队添加：只服务团队池，不引入「无关键词全局列表」

## Risks / Trade-offs

- [大团队首屏列表偏长] → 分页 + 可选搜索；默认 pageSize 适中
- [并发：勾选后他人先加入项目] → 提交时既有添加逻辑应幂等或报错；列表刷新即可
- [项目无 team_id 异常数据] → 接口返回空或 400；UI 空态

## Migration Plan

- 无 DB 迁移
- 前后端同发；旧前端不调新接口无影响

## Open Questions

- 无（探索已拍板：方案 B + 默认 Tab + 共用角色 + 复用添加 API）

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/01-产品需求/产品定位与核心功能.md`（成员与邀请）
- `docs/05-接口文档/接口清单-项目团队成员.md`
- `docs/08-版本记录/2026-11.md`
- `openspec/specs/project-member-without-team/spec.md`（边界对照）
- 拟新增 ADR：无
