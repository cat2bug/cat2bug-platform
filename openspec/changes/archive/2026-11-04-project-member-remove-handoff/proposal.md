## Why

删除项目成员时，系统只移除 `sys_user_project`，不处理该成员仍作为唯一处理人的未终态缺陷，也不清理项目下 Git 仓 ACL。结果是缺陷卡在「幽灵处理人」、仓权限残留，甚至撞上「最后一位 admin 不可删」却没有任何交接路径。需要在移出成员前完成缺陷与仓 admin 的统一交接。

## What Changes

- 项目成员管理删除成员时：扫描本项目未通过、未关闭（非 `RESOLVED` / `CLOSED`）且 `handle_by` 含该成员的缺陷
  - **唯一处理人** = 被删成员 → 弹框指定**统一交接人**（一把梭），全部改派后再删成员
  - **多人处理人**含被删成员 → 静默从 `handle_by` 摘掉该成员，不弹框（若仅有此类则不因缺陷弹框）
- 同步清理本项目下各 Git 仓中该用户的 ACL 行
  - 若某仓该用户是**最后一位 admin** → 同一弹框的统一交接人升为/设为该仓 `admin`，再移除被删用户
  - 非最后 admin → 直接删除 ACL
- 弹框触发条件：存在需改派的唯一处理人缺陷，**或**存在需交接的最后 admin 仓（任一即弹）
- 创建人 / 用例 / 文档 / 计划等其它成员引用本次**不处理**
- 后端以事务保证「缺陷交接 + 仓 ACL 交接/清理 + 删项目成员」原子；不可只靠前端

## Capabilities

### New Capabilities

- `project-member-remove-handoff`: 删除项目成员前的缺陷处理人交接、Git 仓 ACL 同步清理，以及最后 admin 与统一交接人的交接弹框与 API

### Modified Capabilities

- `git-repo-members`: 补充「经项目成员移除路径」时，在保证仓内始终至少一名 admin 的前提下，允许将最后 admin 交接给指定交接人后再删除被移除用户的 ACL（独立仓成员页的「删最后 admin 仍拒绝」不变）

## Impact

- 前端：`cat2bug-platform-ui/src/views/system/project/member/index.vue` 及删除确认/交接弹框；项目成员 API 客户端
- 后端：`SysUserProjectService.deleteMember`、`ProjectResource` 成员 DELETE；缺陷 `handle_by` 批量改写；`SysGitRepositoryMember` 按项目批量清理/交接
- 接口：删除成员 API 扩展为可携带交接人，或拆「预检 + 确认删除」两步；需兼容无交接场景的直接删除
- 不推翻既有 ADR；仓「最后一位 admin」保护原则保留，仅增加合规的交接路径
