# ADR-008 系统管理员对 Git 仓库全权限

记录系统管理员不再受 Git 仓库项目角色限制的决定。

- 日期：2026-10-14
- 状态：已采纳（仅 Quarkus 线；修订 [[ADR-006-Git仓库用JGit自托管与Smart-HTTP]] 的权限表）

## 背景

[[ADR-006-Git仓库用JGit自托管与Smart-HTTP]] 规定「仅系统超管、非项目成员 → 拒绝」，Spring 实现中刻意不检查 `SysUser#isAdmin()`。这与缺陷、用例等其它模块「系统管理员可管理全部」不一致，实际使用中管理员无法排查他人项目的仓库。现象见 [[系统管理员在Git模块无权限]]。

## 候选方案

当时未记录，待补充。

## 最终决定

提交 `59f6a23e`（Quarkus）：`GitRepositoryAccessService.isSystemAdmin(userId)`（超管 userId 或持有 admin 角色）为真时，对任意存在的仓库授予 ADMIN 级别读、写、管理权限；仓库列表同样放行。

## 原因

与其它模块的管理员语义保持一致。

## 副作用

- 系统管理员账号泄露的影响面扩大到全部源码与 force-push，需要更严格保护管理员账号。
- 待确认：Spring 线 `cat2bug-platform-system/.../git/GitRepositoryAccessServiceImpl.java` 未同步，两条后端行为不一致（已不适用：Spring 线于 2026-10-14 删除，见 [[ADR-010-移除Spring后端并统一模块命名]]）；是否同时适用于 Smart HTTP（clone/push）需以测试确认。
