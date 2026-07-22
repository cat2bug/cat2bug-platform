## 1. History 模式与 SPA / Git 隔离

- [x] 1.1 将 `createWebHashHistory` 改为 `createWebHistory`，并梳理 `base`（嵌入式与独立部署）
- [x] 1.2 更新 `buildProjectShareUrl` / path-key 工具及所有依赖 `location.hash` 的白名单/分享逻辑为 History path
- [x] 1.3 扩展 `EmbeddedSpaFallbackFilter`（及单测）排除 `/{team}/{project}/code/{slug}.git/**`；核对 nginx `try_files` 不抢 `.git` 流量
- [x] 1.4 更新/跑 `native-spa-smoke`（或等价）验证无 `#` 深链刷新回 SPA

## 2. 项目命名空间路由骨架

- [x] 2.1 定义模块白名单常量（singular：dashboard/defect/case/plan/report/document/module/notice/member/option/api/tools/code）
- [x] 2.2 注册父路由 `/:teamKey/:projectKey` 与子模块出口；非法第三段 → 404；保留字仍拦截第一段
- [x] 2.3 路由守卫：解析 path keys → 校验权限 → 设置 `currentProjectId` / 上下文；失败 403/404
- [x] 2.4 空路径与 `/dashboard` 挂载项目仪表盘（替换或收敛现有 path-entry 仅跳转的行为）

## 3. 菜单生成与切项目

- [x] 3.1 在 `GenerateRoutes`（或等价）将项目功能菜单改写为带当前 `/{teamKey}/{projectKey}` 前缀的链接
- [x] 3.2 切换项目时按目标 keys 重写当前 module 后缀并导航（前缀必变）；侧栏 activeMenu 同步
- [x] 3.3 系统/团队/认证菜单保持原前缀，不进入项目命名空间

## 4. 源码模块试点（code/{slug}）

- [x] 4.1 仓库列表进入浏览改为 `/{t}/{p}/code/{slug}`；用 slug（+项目）解析 `repositoryId` 调现有 API
- [x] 4.2 迁移 browse / branches / pulls / releases / commits 等深链到 `code/{slug}/**`
- [x] 4.3 旧 `/project/git-repo/browse/:id/**` redirect 到 slug 路径（可解析时）
- [x] 4.4 确认 clone URL 仍为 `.../code/{slug}.git`，与 SPA `code/{slug}` 无冲突

## 5. 其余业务模块与旧路径

- [x] 5.1 迁移 defect/case/plan/report/document 等白名单模块到命名空间子路由
- [x] 5.2 为旧根路径（如 `/defect`、`/plan`…）增加 redirect（有当前项目 keys 时）
- [x] 5.3 处理 member/option/api/tools/notice/module；集成类菜单按 design 并入或暂留并文档化

## 6. 验证与收尾

- [x] 6.1 手工：分享链无 `#`、刷新深链、切项目前缀变化、`.git` clone、SPA 业务页
- [x] 6.2 回归：登录/setup/upgrade/system 页不受项目前缀影响
- [x] 6.3 补充 TESTING 清单（可选）并更新相关 readme/部署说明中的 URL 示例
