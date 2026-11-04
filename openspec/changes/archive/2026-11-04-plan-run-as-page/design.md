## Context

列表页 `views/system/plan/index.vue` 通过 `HandlePlanDialog`（`el-drawer` size 90%）承载执行工作台。项目路由为 `/{teamKey}/{projectKey}/plan/:pathMatch(.*)*` 全部落到同一列表组件；无独立执行页。文档 Markdown 已有「列表 → 整页」先例（`/document/md/:docId`）；代码模块用显式子路由（`buildGitCodeChildren`）避免 catch-all 吞掉深层路径。

## Goals / Non-Goals

**Goals:**

- 名称、「执行」、快捷键、深链进入 `/{teamKey}/{projectKey}/plan/:planId/run` 整页。
- 执行页内容与现抽屉等价（标题区、统计、用例/缺陷切换、模块树）；返回回到计划列表并刷新列表。
- 侧栏仍高亮测试计划模块。

**Non-Goals:**

- 不改后端 API、权限码、用例嵌套执行抽屉（`HandleCaseOfPlan`）。
- 不改新建/编辑计划对话框。
- 不做工作台内嵌整页执行。

## Decisions

1. **路由形态**：仿 `buildGitCodeChildren`，新增 `buildPlanChildren()`：
   - `plan` → 列表 `index.vue`
   - `plan/:planId/run` → 执行页
   - 从通用 `${mod}/:pathMatch(.*)*` 循环中排除 `plan`（与 `code` 相同）。
   - meta：`projectScoped`、`projectModule: 'plan'`、`activeMenu` 指向计划列表路径；`projectListPage: false`（执行页非列表）。

2. **组件拆分**：将 `HandlePlanDialog` 正文抽为可复用面板（或直接把该组件改为整页视图）。列表不再挂载执行抽屉；执行页 `mounted`/`watch $route.params.planId` 加载计划。关闭/返回：`router.push` 到同项目 `plan` 列表，并在需要时触发列表刷新（`onActivated` / query 标记 / 返回前 emit 均可，优先返回后列表 `activated`/`getList`）。

3. **导航**：列表用 `buildProjectModulePath(teamKey, projectKey, 'plan', planId, 'run')`。仪表盘 `PlanStatisticsChart` 改为打开该路径（可用 `window.open` 或同页跳转，保持新标签行为若现网如此）。

4. **旧深链**：列表 `mounted` 若仍有 `?planId=`，`replace` 到 `plan/:planId/run`，去掉 query，避免再弹抽屉。

5. **键盘**：执行页继续复用计划执行关闭/快捷键逻辑；Esc 先关嵌套用例抽屉，再返回列表（与现「先关嵌套再关抽屉」一致，只是外层变为路由返回）。

## Risks / Trade-offs

- [Risk] catch-all 未排除 `plan` → `plan/:planId/run` 仍进列表 → Mitigation：显式子路由且 filter 掉 plan。
- [Risk] keep-alive 下列表不刷新 → Mitigation：返回时 `getList` 或列表 `activated` 刷新。
- [Trade-off] 不再「半透明盖住列表」；用户需返回才能看到列表，换取更大工作区与可分享 URL。

## Migration Plan

- 纯前端发布；无 DB/API 迁移。
- 回滚：恢复抽屉挂载与列表 `open()`、移除 plan 专用子路由即可。

## Open Questions

- 无（用例嵌套抽屉保留已确认）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（对话框/抽屉与键盘关闭）
- `docs/00-项目总览.md`
- 拟新增 ADR：无（交互形态调整，不上升为跨功能长期决策；若后续多模块统一「重操作改整页」再立 ADR）
