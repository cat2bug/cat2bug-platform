## 1. 后端：预检与删除交接

- [x] 1.1 实现删除成员影响预检（sole-handler 未终态缺陷、multi-handler 计数、last-admin 仓、其它 ACL 仓、`requiresHandoff`），并在 `ProjectResource` 暴露接口
- [x] 1.2 扩展 `deleteMember`：`requiresHandoff` 时校验 `handoffUserId`；事务内改派 sole-handler、静默摘 multi-handler、仓 ACL 交接/清理、再删 `sys_user_project`
- [x] 1.3 缺陷改派：sole → `handle_by=[handoff]` 且状态不变并写移除改派日志；multi → 仅移除被删 userId；终态不改
- [x] 1.4 Git ACL：非最后 admin 直接删；最后 admin 先确保交接人为 `admin` 再删被删人；独立仓成员页删最后 admin 行为不变
- [x] 1.5 项目成员移除路径下的仓 ACL 写操作鉴权与项目级 remove 权限对齐（避免要求每仓 manage）

## 2. 前端：成员管理交接弹框

- [x] 2.1 `project.js` 增加预检与带 `handoffUserId` 的删除 API
- [x] 2.2 `member/index.vue`：删除前预检；无需交接走原确认；需交接弹出框展示缺陷/仓摘要并单选交接人
- [x] 2.3 交接弹框文案与校验（必选交接人、排除被删人）；成功刷新列表；取消不删人
- [x] 2.4 补充 i18n（中/英若项目惯例需要）

## 3. 验证

- [x] 3.1 手工或接口验证：sole-handler / multi-handler / last-admin / 无交接 / 缺 handoff 拒绝 / 仓成员页仍拒删最后 admin（后端相关模块 `mvn compile` 通过；仓页 `deleteMember`/`rejectIfLastAdmin` 未改；端到端请在成员管理页点删复核）

## 4. 回写长期知识

- [x] 4.1 在 `docs/03-技术方案/权限模型与鉴权链路.md` 补充「项目成员移除旁路仓 manage」约定
- [x] 4.2 更新 `docs/05-接口文档/接口清单-项目团队成员.md` 预检与带 `handoffUserId` 的删除说明
