# TESTING — team-project-path-keys

## 前置

- 后端已执行 Flyway `V1_1_0_4__team_project_path_keys`（存量团队/项目有 `team-{id}` / `project-{id}` 回填；本地若已有 game 的 `1.1.0.2`/`1.1.0.3`，勿再用 `_2` 版本号）
- 登录账号具备团队创建、项目创建、项目成员权限

## 1. 创建路径键

1. 新建团队：填显示名 + `teamKey`（如 `cat2bug`）→ 成功  
2. 再用同 `teamKey` 创建 → 失败（重复）  
3. `teamKey=login`（保留字）→ 前端/后端拒绝  
4. 在该团队下新建项目：填 `projectKey`（如 `cat2bug-platform`）→ 成功  
5. 同团队再建同 `projectKey` → 失败；另一团队可用相同 `projectKey`

## 2. 分享链接 → 仪表盘

> **History 更新（`project-scoped-spa-routes`）**：规范 URL 已无 `#`。完整 History / 模块深链清单见 `openspec/changes/project-scoped-spa-routes/TESTING.md`。

1. 浏览器打开 `http://{host}/cat2bug/cat2bug-platform`（History 路径；勿再使用 `/#/...`）  
2. 已登录且为项目成员 → 切换当前项目并进入 **Dashboard**  
3. 未登录 → 登录页；非成员 → 401/403；错误 key → 404  
4. 打开 `/login` 等保留路径 → 不被动态路由抢走

## 3. Clone URL

1. 源码管理页复制 clone 地址，形态为：  
   `http://{apiHost}/cat2bug/cat2bug-platform/code/{slug}.git`  
2. `git clone` + 平台账号 HTTP Basic → 成功（有 pull 角色）  
3. 旧地址仍可用（兼容窗）：  
   `http://{apiHost}/git/p/{projectId}/{slug}.git`

## 4. 改名警告

1. 编辑团队/项目路径键 → 出现断链确认（分享链接与 clone URL）  
2. 确认后旧 `/{oldTeam}/{oldProject}` 与旧 namespaced clone 失效；legacy `/git/p/{id}/...` 仍可用

## 自动化

```bash
mvn -pl cat2bug-platform-system,cat2bug-platform-framework,cat2bug-platform-admin -am test \
  -Dtest=SysProjectPathResolveServiceTest,GitPackAuthorizationFilterTest,GitSmartHttpPathSupportTest,SysGitRepositoryCloneUrlTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```
