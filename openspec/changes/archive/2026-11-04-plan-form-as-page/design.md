## Context

计划新建/编辑由列表与工作台挂载的 `AddPlanDialog`（`el-dialog` 80%）完成：上方元数据表单，下方「选用例」区已用 `vue-multipane` + `TreePlanItemModule`（交付物/优先级）+ 用例多选表。执行工作台已在 `plan-run-as-page` 中改为 `plan/:planId/run` 整页；`buildPlanChildren()` 目前仅含列表与执行页。弹层视口限制导致分栏与用例页整页体验不一致。

## Goals / Non-Goals

**Goals:**

- 新建：`/{teamKey}/{projectKey}/plan/new`；编辑：`/{teamKey}/{projectKey}/plan/:planId/edit`，整页等价于原对话框能力。
- 选用例区布局与交互对齐用例页：左侧交付物树、中缝可拖、宽度/显隐缓存；`deliverable` 团队模块闸门行为保持。
- 列表「新建/修改」、工作台快捷创建等入口改为路由跳转；保存成功、取消/返回均回到计划列表并刷新。
- 页边距与计划列表一致（外层左右约 20px）。

**Non-Goals:**

- 不改后端 API、计划/PlanItem 数据模型、权限码语义。
- 不改执行整页（`plan/:planId/run`）业务逻辑。
- 不把保存后默认导航改为执行页。
- 不把左侧换成用例页的 `TreeModule`（计划侧继续 `TreePlanItemModule`，只对齐布局手感）。
- 不新增「计划级交付物持久化字段」；交付物仍只服务筛选与批量勾选 case。

## Decisions

1. **路由扩展 `buildPlanChildren()`**  
   增加：
   - `plan/new` → 新建页（须写在 `plan/:planId/*` 之前，避免 `new` 被当成 planId）
   - `plan/:planId/edit` → 编辑页  
   与现有 `plan`、`plan/:planId/run` 并列。meta：`projectScoped`、`projectModule: 'plan'`、`activeMenu` 指向计划列表；`projectListPage: false`；建议 `noCache: true`（与执行页一致，避免 keep-alive 脏表单）。  
   **备选**：单一 `plan/form` + query `mode`/`planId` — 否决，不利于深链与权限区分。

2. **组件形态**  
   将 `AddPlanDialog` 正文抽为可路由页面组件（如 `views/system/plan/form.vue` 或复用改造后的面板），按路由区分 `mode: create | edit`：新建走临时 planId 拉可选用例；编辑 `getPlan` 回填。页头：返回、标题、取消/保存。列表与 `QuickCreate` 去掉 Dialog 挂载（或保留组件但不作为主入口）。  
   **备选**：整页外包一层空 Dialog — 否决。

3. **左右分栏**  
   继续 `vue-multipane` + `multipane-resizer-grip` + 既有 mixin（`paneResizerHandleViewport`、`multipaneTreeTableHeightSync`）；宽度键可沿用或改为页面专用 session 键（避免与弹窗时代冲突则换键名）。样式参考 `views/system/case/index.vue` 的 `.custom-resizer` / `.tree-module`，高度改为整页剩余视口而非 dialog body。

4. **导航与保存**  
   - 保存成功：`addPlan`/`updatePlan` 成功后 `router.push` 到同项目 `plan` 列表；列表 `activated`/`onActivated` 或返回 query 标记触发 `getList`。  
   - 取消/返回：同上回列表。  
   - 未保存离开：将原 Dialog `before-close` / 键盘逻辑迁到 `onBeforeRouteLeave`（有改动才确认）；无既有脏检查则保持与现 Dialog 同等严格度，不额外发明复杂 diff。

5. **权限**  
   新建页需新增权限、编辑页需修改权限；无权限访问 URL 时按项目其它表单页惯例拦截/提示（与菜单权限一致）。

## Risks / Trade-offs

- [Risk] 路由顺序错误导致 `/plan/new` 落入 `plan/:planId/run` 或 edit — Mitigation：显式注册顺序 + 单测扩展 `project-scoped-routes`。
- [Risk] 整页高度测量错误导致表体双滚动或空白 — Mitigation：对齐用例页 multipane 高度同步，回归交付物开/关与树折叠。
- [Risk] keep-alive 残留未提交表单 — Mitigation：`noCache` 或离开时重置。
- [Trade-off] 失去「列表上浮层编辑」；换取与执行页一致的整页与可分享 URL。

## Migration Plan

- 纯前端；无 DB/API 迁移。
- 回滚：恢复列表/工作台挂载 `AddPlanDialog`，移除 `plan/new` 与 `plan/:planId/edit` 子路由。

## Open Questions

- 无（编辑亦整页、保存后回列表已确认）。未保存离开确认对齐原 Dialog 行为即可。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`
- `docs/00-项目总览.md`
- 拟新增 ADR：无（与执行整页同属交互形态调整；若多模块统一「重表单改整页」再立 ADR）
