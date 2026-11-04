## Context

评审批次已存在（`sys_case_review` / `sys_case_review_item`，状态 `open` | `completed`），完成时按团队纪要模板渲染并写入 `report_markdown`。用例页仅有「发起评审」按钮与 `CaseReviewDialog`；`GET /system/case-review?projectId=` 返回全量瘦列表（无筛选、分页、摘要统计），前端无历史页与导出。计划模块已有 `buildPlanChildren` 整页子路由先例，可对齐到 `case/reviews`、`case/reviews/:reviewId`。

约束：不推翻 [[ADR-023]]（模板归属团队）；评审仍为可选勾选批次；导出内容为落库纪要快照，不以最新模板重渲。

## Goals / Non-Goals

**Goals:**

- 用例工具栏 split：主点发起（依赖勾选），箭头始终可进「评审记录」。
- 独立列表页 + 详情页（项目命名空间，不进侧栏）。
- 列表筛选（状态、时间、可选发起人）+ 摘要列；查看；已完成导出 md/doc/pdf（快照）。
- 列表 API 可筛选/分页并带统计摘要，避免 N+1。

**Non-Goals:**

- 多人会签 / 多评审人模型改造。
- 服务端强制 PDF 引擎或新对象存储。
- 侧栏新增「评审记录」菜单项。
- 工作台待评审深链（可后续增量）。
- 改变「完成才写库 / 保存到文档」既有语义。

## Decisions

### D1. 入口：split-button，主禁用、箭头可用

- **选择**：对齐用例「新建」的 `el-dropdown split-button`；主按钮 `@click` → 现有 `handleStartReview`（未勾选 disabled）；下拉两项「发起评审」「评审记录」。箭头与「评审记录」不依赖勾选。
- **备选**：整组随勾选禁用 → 无法进历史，否决。

### D2. 路由：`case` 子路由，仿 `buildPlanChildren`

- **选择**：`case`（列表）、`case/reviews`（历史列表）、`case/reviews/:reviewId`（详情）。`reviews` 静态段须排在可能吞路径的通配之前（若有）。
- **备选**：抽屉挂在用例页 → 与「独立页」决策不符。

### D3. 列表 API 扩展，非新资源

- **选择**：扩展 `GET /system/case-review`：`projectId` 必填；可选 `status`（`open`|`completed`）、时间范围（建议 `createTimeBegin`/`createTimeEnd` 或完成时间成对参数，实现时二选一并在接口清单写清）、`createBy`；分页按项目列表惯例（`pageNum`/`pageSize`）；每行附 `itemCount` / `passedCount` / `changesCount` / `pendingCount`（SQL 聚合或一次 join 统计）。
- **详情**：继续 `GET /system/case-review/{reviewId}`（含 items）。
- **备选**：每行前端再拉详情 → N+1，否决。

### D4. 导出 = 快照格式转换，客户端优先

- **选择**：仅 `status=completed` 且有 `report_markdown` 时可导出。md：原文下载；doc：Markdown→HTML→现有 `html-docx`；pdf：同 HTML 路径（打印预览或轻量客户端转换，实现时选一种并在 tasks 落地）。不按当前团队模板重渲。
- **与「保存到文档」**：并存；导出本地下载。
- **备选**：服务端 PDF → 依赖与体积大，首版不做。

### D5. 详情页行为

- **completed**：展示 createBy/completeBy、时间、统计、用例表、纪要只读预览（若已有改纪要权限与 API，可保留「编辑纪要」入口，不扩大权限模型）。
- **open**：同上摘要 + 用例结论表；提供「继续评审」打开现有 `CaseReviewDialog`（或等价）完成流程。
- **权限**：查看 `system:case:query`（与现 list 一致）；发起/继续完成仍 `system:case:review`。

### D6. 不新开 ADR

交互与路由形态对齐计划整页先例；模板与快照语义已由 ADR-023 与现 `case-review` 覆盖。拟新增 ADR：无。

## Risks / Trade-offs

- [历史 open 脏数据] → 列表可筛 `open`；详情可继续完成或仅查看；不在本变更强制清理。
- [pdf 质量不一致] → 首版可接受「打印另存为 PDF」或简单 HTML→PDF；在 tasks 标明实现选型。
- [列表聚合性能] → 项目内评审批次量通常不大；分页 + 聚合索引按 `project_id`；必要时再优化。
- [createBy 为 username] → 列表/详情与现网一致展示账号名；不引入成员解析除非已有工具函数可复用。

## Migration Plan

- 无 DB 迁移（表结构已有）；仅 API 查询与前端路由。
- 回滚：去掉子路由与工具栏 split，恢复单按钮；扩展查询参数向后兼容（无参仍可返回列表）。

## Open Questions

- 时间筛选字段默认用「发起时间」还是「完成时间」：实现默认 **发起时间** `createTime`；完成时间可作为次要筛选项若成本低。
- PDF 具体库：实现阶段在前端现有能力中选型，不阻塞本设计。

## 相关长期文档

- `docs/01-产品需求/术语表.md`（用例评审）
- `docs/01-产品需求/产品定位与核心功能.md`（测试用例 / 评审）
- `docs/03-技术方案/用例评审纪要模板.md`（占位符；归档前补全与代码一致）
- `docs/04-UI规范/组件与交互约定.md`（列表页工具条、Cat2BugTable）
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`（case-review）
- `docs/07-技术决策/ADR-023-用例类型评审模板与缺陷属性归属团队.md`
- `docs/07-技术决策/ADR-019-可视化Markdown统一用Tiptap连续A4纸面.md`（导出旁路，不绑死编辑器核）

拟新增 ADR：无。
