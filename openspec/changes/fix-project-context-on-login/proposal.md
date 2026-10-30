## Why

团队成员只要 `sys_user_config.current_project_id` 仍指向某项目（陈旧、被误写、或可绕过成员校验切进去），侧栏就会因团队角色自带「工作台 / 缺陷」菜单而显示项目区，且缺陷等接口往往只验功能权限、不验 `sys_user_project` 成员关系，导致**非项目成员仍可查看该项目数据**。另：账号在当前团队下没有任何可访问项目时，登录后应落到项目列表，且不显示工作台与缺陷入口——现状缺少这套着陆编排。

## What Changes

- **登录 / GetInfo 清洗**：若 `currentProjectId` 对应项目对当前用户不是成员（或项目不存在），服务端清零并写回；响应里不再带回无效项目上下文。
- **写配置硬闸**：`PUT /system/user-config`（含切项目）写入非零 `currentProjectId` 时必须校验项目成员；非成员拒绝（403），不得靠前端 `getProject` 失败后的兜底静默写入。
- **读项目硬闸**：按 ID / path key 取「可进入的当前项目」时，非成员不得当作可访问成功（修复 LEFT JOIN 导致的假成功）；命名空间路由进入非成员项目时拒绝并离开该上下文。
- **项目业务读闸（本变更范围）**：至少覆盖复现路径上的缺陷列表/详情与工作台落地所需接口——非成员不得凭功能权限 + `projectId` 读出数据。
- **移出成员清理**：从项目移除成员时，若其 `current_project_id` 正是该项目，清为零。
- **无项目着陆**：登录后（或 GetInfo 清洗后）当前团队下无任何可访问项目时，前端跳转项目列表页；侧栏**不**显示工作台、缺陷等项目模块菜单（`currentProjectId` 为空/0）。
- **非目标**：不改团队角色菜单种子（`team.default` 仍可挂项目菜单模板）；不改甘特计划节点 ACL；不一次性给全站所有模块补成员校验（Git ACL 等已有专用闸门的保持不动）。

## Capabilities

### New Capabilities

- `project-context-guard`：当前项目上下文的合法性（成员校验、配置清洗、无项目着陆、侧栏项目区显隐、移出成员清配置）。

### Modified Capabilities

- `project-scoped-module-routes`：非项目成员访问 `/{teamKey}/{projectKey}/**` 时不得建立当前项目上下文或渲染业务页，须拒绝并离开该命名空间。

## Impact

- 后端：`SysUserConfigService` / `UserConfigResource`、`UserInfoResource.getInfo`、`SysProjectService`/`SysProjectMapper`（按 ID 可读性）、缺陷列表等资源的成员校验、`SysUserProjectService.deleteMember`。
- 前端：`stores/user.js`（`SwitchCurrentProject` 失败兜底）、`permission.js` / 登录后跳转、`Sidebar` 项目区显隐、`ensureProjectScopedContext` 对 403 的处理。
- 文档：回写 `docs/03-技术方案/权限模型与鉴权链路.md`、必要时 `docs/05` 用户配置/项目接口一句；不新开 ADR（延续「项目数据以成员关系为边界」既有模型）。
