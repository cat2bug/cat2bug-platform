## Context

项目成员删除现状：前端 `member/index.vue` 确认后调用 `DELETE /system/project/{projectId}/member/{memberId}`；`SysUserProjectService.deleteMember` 仅删 `sys_user_project` 并清理当前项目缓存，不碰缺陷 `handle_by`、也不碰 `sys_git_repository_member`。

缺陷 `handle_by` 为 MySQL JSON 数组（`List<Long>`）。未终态：`CREATED` / `PROCESSING` / `AUDIT` / `REJECTED`；终态：`RESOLVED`(通过)、`CLOSED`(关闭)。仓 ACL「最后一位 admin」由 `SysGitRepositoryMemberServiceImpl.rejectIfLastAdmin` 保护；独立仓成员页删除仍须拒绝「删光 admin」。

## Goals / Non-Goals

**Goals:**

- 删除项目成员前完成未终态缺陷处理人交接（唯一 → 弹框一把梭；多人 → 静默摘掉）
- 同步清理本项目下各仓该用户 ACL；最后 admin 仓由同一统一交接人接任 admin 后再删
- 后端事务原子；预检可告知前端是否需要弹框及影响摘要

**Non-Goals:**

- 不改缺陷创建人、用例、文档、计划、工作台等其它成员引用
- 不改仓成员设置页自身的「删最后 admin」拒绝行为（无交接人时仍拒绝）
- 不做逐条缺陷不同交接人；不做多仓各自不同 admin
- 不在本次引入新 ADR（原则沿用既有仓 admin 保护 + 成员管理权限）

## Decisions

### D1. API：预检 + 带交接人的确认删除

- **预检** `GET`（或 `POST` preview）返回：
  - `soleHandlerDefects`：未终态且唯一处理人为被删用户的缺陷摘要（编号、标题、状态）
  - `multiHandlerDefectCount`：需静默摘掉的缺陷数（可选展示）
  - `lastAdminRepositories`：被删用户为最后 admin 的仓列表
  - `otherAclRepositories`：可直接摘 ACL 的仓列表（可选）
  - `requiresHandoff`：`soleHandlerDefects` 或 `lastAdminRepositories` 非空则为 true
- **确认删除** 在现有 `DELETE .../member/{memberId}` 上扩展 body/query：`handoffUserId`（当 `requiresHandoff` 时必填）；无交接需求时可无 body，行为兼容旧客户端但服务端仍执行静默摘 handle_by + 非最后-admin ACL 清理。
- **备选否决**：仅前端 list 缺陷再循环 `assign` —— 无法保证原子，且绕过前端仍留孤儿。

### D2. 统一交接人语义

- 弹框选**一名**项目成员（排除被删用户与当前操作者约束与现有删除规则一致：不能删自己；交接人必须仍是项目成员）。
- 该用户同时用于：
  1. 所有 sole-handler 未终态缺陷：`handle_by` **整表替换**为 `[handoffUserId]`（与 `assign` 整表替换一致，状态不变；写缺陷日志，备注标明因成员移除改派）
  2. 每个 last-admin 仓：若交接人已有 ACL → 升为 `admin`；若无 → 插入 `admin`；然后再删被删用户 ACL（此时仓内至少还有交接人 admin，不触发 last-admin 拒绝）

### D3. 多人处理人与无交接路径

- 未终态且 `handle_by` 含被删用户且 length ≥ 2：从数组移除该 userId 后写回；不弹框、不要求 `handoffUserId`。
- 若仅有此类 + 非最后-admin 仓 ACL：确认后直接执行，无弹框。
- 终态缺陷（通过/关闭）中的 `handle_by`：**不改**（历史快照）。

### D4. 事务与权限

- 单事务顺序建议：校验权限与「不能删自己 / 不能删项目创建人角色」→ 校验 `handoffUserId`（若需要）→ 改缺陷 → 仓 ACL 交接/清理 → 删 `sys_user_project`（及角色清理若一并修脏数据可顺带）→ 清当前项目缓存。
- 权限：沿用 `system:project:member:remove`；交接写缺陷/仓 ACL 在服务端以成员移除上下文执行，避免要求操作者同时具备每仓 manage（否则项目管理员可能删不了人）。需在实现中显式「项目成员移除旁路」或复用已有项目级管理权限判定，并在 design 落地时与 `GitRepositoryAccessService` 对齐。

### D5. 前端交互

- 点删除 → 调预检：
  - `requiresHandoff=false` → 现有确认框 → 确认删除
  - `requiresHandoff=true` → 交接弹框：展示受影响缺陷数/列表摘要、需交接 admin 的仓名；`SelectProjectMember` 单选交接人（必填）→ 提交带 `handoffUserId` 的删除
- 文案需说明：交接人将接管所列缺陷，并成为所列仓的 admin。

## Risks / Trade-offs

- [大批量 sole-handler 缺陷] → 一把梭可能误派；缓解：弹框列出编号摘要与数量，确认文案强调「全部交给同一人」
- [交接人随后也被删] → 同一流程递归处理；无额外特殊路径
- [项目成员移除旁路仓 manage] → 权限放大风险；缓解：仅在「删除项目成员」事务内、目标用户必须是被删成员、交接人必须是项目成员，审计日志写明原因
- [旧客户端不传 handoff] → 服务端在 requiresHandoff 时返回明确错误码，前端必须升级；旧调用失败优于留下孤儿

## Migration Plan

- 无 DB schema 变更（复用现有 JSON / ACL 表）
- 发布前后端同版本；先上后端（缺 handoff 时拒绝需交接的删除）再上前端亦可
- 回滚：恢复旧删除逻辑；已交接数据不自动回滚

## Open Questions

- 无（探索阶段已拍板：A 静默摘多人、一把梭、Git 同步、最后 admin 由同一交接人接任）

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/02-系统架构/数据库.md`（`sys_defect.handle_by`、`sys_git_repository_member`）
- `docs/03-技术方案/权限模型与鉴权链路.md`（仓 ACL 优先于项目角色）
- `docs/05-接口文档/接口清单-Git代码仓库.md`
- `docs/07-技术决策/ADR-006-Git仓库用JGit自托管与Smart-HTTP.md`
- `docs/07-技术决策/ADR-008-系统管理员对Git仓库全权限.md`
- 拟新增 ADR：无
