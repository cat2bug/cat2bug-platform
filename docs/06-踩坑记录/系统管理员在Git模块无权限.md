# 系统管理员在 Git 模块无权限

记录 Git 仓库权限与其它模块不一致的问题。

- 日期：2026-10-14
- 涉及：Git 模块 / Quarkus 后端

## 现象

系统管理员（`admin`）能看、能改所有项目的缺陷、用例等，但进入不是自己所在项目的代码仓库时被拒绝，无法浏览、clone 或管理。

## 根因

Git 权限按「仓级 ACL 优先，否则回落项目角色」判定，设计时刻意不检查 `SysUser#isAdmin()` / `team.admin`（见 `2026-07-21-project-git-repositories/design.md` D3：「仅系统超管 → 拒绝」）。于是系统管理员不是项目成员时被当作普通用户。

## 解决办法

Quarkus 线（提交 `59f6a23e`）：`GitRepositoryAccessService` 新增 `isSystemAdmin(userId)`（超管 userId 或持有 admin 角色），`GitRepositoryAccessServiceImpl` 判定权限时先放行为仓库 ADMIN（读、写、管理全开）；`SysGitRepositoryServiceImpl` 列表同样放行。决策见 [[ADR-008-系统管理员对Git仓库全权限]]。

## 以后怎么避免

- 新模块做权限时，先对齐「系统管理员是否全放行」的全局约定。
- 待确认：Spring 线 `cat2bug-platform-system/.../git/GitRepositoryAccessServiceImpl.java` 仍保留「刻意不检查 isAdmin」，两条后端行为不一致，是否需要同步。（已不适用：Spring 线于 2026-10-14 删除，见 [[ADR-010-移除Spring后端并统一模块命名]]。）
