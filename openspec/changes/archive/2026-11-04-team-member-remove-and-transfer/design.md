## Context

团队成员页（`views/system/team/option/member/index.vue`）今日仅有锁定/解锁：写 `sys_user_team.team_lock`，人仍在队。无 DELETE 成员 API。`GET /system/team/my` 只认 `sys_user_team`。项目列表「全部项目」按当前 `teamId` 列全部项目；「我参与的」靠 `memberOnly` + `sys_user_project`。

既有能力 `project-member-without-team` 已定义「仅项目挂职」：侧栏团队入口隐藏、团队写 API 403；但规定团队切换器完全不可用——与本次「离队后仍可选有挂职的团队」冲突，需改写该条。

团队创建人角色：`team.create-by`（种子 `role_id=12`）。套餐人数见 ADR-024 / `admin-team-plan-quota`。

## Goals / Non-Goals

**Goals:**

- 用「删除」取代成员锁定 UI；删除只拆队籍，保留项目挂职，并提示操作者。
- 创建人不可删；提供「转让」把 `team.create-by` 交给另一正式队员，始终一名创建人。
- 我的团队列表 = 正式队籍 ∪ 该队下有项目挂职；仅项目挂职者项目列表只见「我参与的」。
- 仅项目挂职：禁建项目 / 邀团队成员 / 改团队配置（前后端一致）。

**Non-Goals:**

- 不在本次做「离队并移出全部项目」捆绑流；与 `project-member-remove-handoff` 独立。
- 不删除 `team_lock` 列（可保留兼容）；不清理历史锁定数据的强制迁移（实现时可忽略 lock 展示）。
- 不改平台 Admin 锁团队/锁项目。
- 不引入新 ADR（边界落在既有「仅项目挂职」与配额模型上微调）。

## Decisions

1. **删除语义**  
   `DELETE /system/team/{teamId}/member/{memberId}`：事务内删 `sys_user_team_role` + `sys_user_team`；禁止删持有 `team.create-by` 者；禁止删自己若会导致队无创建人（转让先行）。不碰项目成员/缺陷/Git ACL。  
   *备选：软删/再锁* → 否决，产品要腾配额与真离队。

2. **转让语义**  
   `POST /system/team/{teamId}/transfer-creator`（或 PUT），body：`targetUserId`。校验：调用者为当前创建人（默认**仅创建人本人可转让**）；目标为同队正式成员且非自己。事务：受让人角色改为仅 `team.create-by`；原创建人去掉该角色，若无其它团队角色则落到 `team.admin`。  
   *备选：管理员代转让* → 首版不做，降低权限面。

3. **我的团队并集**  
   改 `selectSysTeamListByUserId` / count：  
   `EXISTS user_team` OR `EXISTS (project in team AND user_project)`。返回行可带 `formalMember: boolean`（或前端另调上下文），供藏「全部项目」。

4. **上下文标志**  
   切换团队或拉项目列表前，暴露「当前用户对当前团队是否正式队员」：优先复用/补齐 `teamMember`（按**当前 teamId** 计算，而非全局）。仅项目挂职 → `teamMember=false`：TeamSelect 仍可选并集中的队；项目页隐藏 `all-project` Tab。

5. **UI**  
   成员行：非创建人 → 删除；创建人 → 转让。删除确认含「将保留其在本团队下的项目成员身份」。锁定/解锁按钮与划线样式移除。

6. **配额**  
   `max_members` 只计有效 `sys_user_team` 行（可继续排除 `team_lock=1` 若字段残留）；删除后立即腾位；回队走既有 invite/create/团队添加并校验配额。

## Risks / Trade-offs

- [离队挂职仍可选队] → 与旧 spec「不可切换团队」冲突 → 以本变更 MODIFIED 为准，归档时合并主 spec。  
- [误删以为踢出项目] → 确认文案 + 文档。  
- [转让目标无正式队籍] → API 拒绝仅项目挂职用户。  
- [创建人误操作转让] → 二次确认；转让后原创建人可再被删，需运营知晓。  
- [历史 team_lock=1] → UI 不再展示锁定；计数与列表视作在队直至被删。

## Migration Plan

- 无强制 DB 迁移；新 API + 前端替换即可。  
- 回滚：恢复锁定按钮并停用 DELETE/transfer（列仍在）。  
- 发布说明：告知管理员改用删除/转让。

## Open Questions

- 无（转让调用者限定创建人本人、受让人须正式队员、转让后原创建人留队为团队管理员——已拍板）。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/05-接口文档/接口清单-项目团队成员.md`
- `docs/06-踩坑记录/ApiException-403文案被SecurityResponses盖掉.md`
- `docs/06-踩坑记录/转让角色后会话权限未重算.md`
- `docs/07-技术决策/ADR-024-团队套餐配额与项目存储上限.md`
- `docs/07-技术决策/ADR-023-用例类型评审模板与缺陷属性归属团队.md`（团队写权限角色）
- 拟新增 ADR：无
- 版本记录：`docs/08-版本记录/2026-11.md`
