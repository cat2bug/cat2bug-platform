## Context

缺陷页已用 `defectContentComponent` 在 `DefectTable` / `DefectExcel` / `DefectGantt` 间切换，共用父级 `queryParams` 与页签筛选；切视图清空勾选。状态流转走 `POST /system/defect/{id}/{assign|repair|reject|pass|close|open}`，前端可见性集中在 `defect-action-visibility.js`，弹框在 `DefectTools/*Dialog` 与 `DefectBatchActionDialog`。产品要在甘特后增加「看板」第四视图：三泳道按状态推进，拖拽落点映射既有动作，权限与表格按钮严格一致。

## Goals / Non-Goals

**Goals:**

- 视图入口「看板」与表格/Excel/甘特同级；本地记忆扩展白名单。
- 三泳道分桶与卡片展示；跨泳道拖拽 → 动作映射 → 必要时弹框 → 成功后刷新分桶。
- 多选整组拖；严格「每条均可」；非法回弹。
- 指派：卡片入口 + 多选批量指派，复用现有弹框。
- 待验证→已结束智能优先：`pass` 优先于 `close`。

**Non-Goals:**

- 新后端权限码、新状态、改 `requireTransition`。
- 泳道内自定义排序持久化；PROCESSING↔REJECTED 列内互拖。
- 拖到成员 chips 改处理人（二期可选）。
- WIP 限制、泳道自定义配置、与工作台旧「看板」页签任何关联。
- 引入第三方看板库（可自研 + 原生/轻量 DnD）。

## Decisions

1. **挂载方式对齐甘特**  
   - `index.vue`：`DEFECT_CONTENT_VIEW_ALLOWED` + radio（甘特后）+ `components` 注册 `DefectKanban`；实现 `list/kanban.vue`；`left-tools` slot 承接同一套筛选。  
   - 备选否决：嵌在甘特内二级 tab（破坏「同级视图」约定）。

2. **拖拽 = 流转，不写 `PUT /system/defect` 改 `defectState`**  
   - 避免绕过日志与处理人副作用；与 MCP/表格语义一致。  
   - 落点映射：

| 源泳道 | 目标泳道 | 动作选择 |
|--------|----------|----------|
| 处理中 | 待验证 | `repair` |
| 待验证 | 处理中 | `reject` |
| 处理中 | 已结束 | `close`（须 `close`） |
| 待验证 | 已结束 | 若当前用户对**每一条**均可 `pass` → `pass`；否则若每一条均可 `close` → `close`；否则拒绝 |
| 已结束 | 处理中 | `open` |
| 同泳道 | 同泳道 | 不改状态（可忽略排序反馈）；指派走菜单/批量，不靠同列拖 |

3. **可见性与批量求交**  
   - 单卡：`isDefectActionVisible`。  
   - 多选：`getBatchActionFlags` 同语义（每条都 true）；混合态拖「待验证→已结束」时若不能统一 `pass` 则尝试统一 `close`。  
   - 后端仍只校验源态+权限码；前端身份卡控与表格一致。

4. **弹框策略**  
   - 单卡：直接挂载/调用与 DefectTools 相同的 Dialog 组件。  
   - 多卡：复用或抽取与 `DefectBatchActionDialog` 相同的串行提交；取消则不调 API、UI 回原列。  
   - `repair`/`assign` 必填人员；`reject`/`close`/`open` 必填描述（与现规则一致）。

5. **数据加载**  
   - 首版：在当前筛选下用较大 `pageSize`（建议 200，常量可配）或甘特式分段触底续拉，客户端按 `defectStateName` 分桶；列头显示本列已加载数。  
   - 不新增专用 list API；若性能不够再开变更。  
   - `RESOLVED`：不入三列；若筛选结果含该态，可在底部弱提示「N 条已解决未展示」或不提示（实现选不提示以减噪）。

6. **卡片与指派 UX**  
   - 卡面：`#projectNum`、标题、类型色、状态角标（驳回）、处理人头像、可选优先级。  
   - 单击选中 / Ctrl·⌘ 加减 / Shift 范围（同列优先）；拖动手势与点击打开详情需防误触（mousedown 阈值或手柄区）。  
   - 指派：卡上「…」或工具图标 → AssignDialog；多选时父级或看板内显示批量指派（不强制改 `defectBatchToolbarVisible` 仅表格的既有规则——看板内自管选中与批量入口，避免破坏「Excel/甘特隐藏批量条」约定）。

7. **不新增 ADR**  
   - 前端视图扩展 + 复用流转，无跨版本架构约束。

## Risks / Trade-offs

- [大筛选结果一次 200 仍不够] → 分段续拉 + 提示缩小筛选；列内虚拟滚动。  
- [pass/close 智能优先被误解成「总能通过」] → 无 pass 资格时明确走关闭并弹必填描述；拒绝时 toast 说明缺权限或非处理人。  
- [多选混合源泳道] → 仅当映射出的**单一动作**对每条都合法才允许；否则回弹。  
- [拖拽与打开详情冲突] → 区分拖动手势阈值；双击或标题点击进详情。  
- [看板自管多选 vs 父级 selectedDefects] → 看板不依赖表格批量条；切走视图仍清空父级选中。

## Migration Plan

- 纯前端 + 既有流转 API；无 Flyway。  
- 回滚：从白名单与 radio 去掉看板即可。

## Open Questions

- （无阻塞）列内虚拟滚动库选型实现时再定；默认先 DOM 列表 + 列滚动。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/01-产品需求/术语表.md`（缺陷状态）
- `docs/04-UI规范/组件与交互约定.md`（缺陷内容视图切换；合入后补看板）
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`（list 与流转）
- `docs/07-技术决策/说明.md`（本次不新增 ADR）
- 拟新增 ADR：无
