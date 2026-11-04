## 1. 后端列表与摘要

- [x] 1.1 扩展 `SysCaseReviewMapper` / XML：按 `projectId` + 可选 `status`、发起时间范围、`createBy` 查询；分页；聚合 `itemCount` / `passedCount` / `changesCount` / `pendingCount`
- [x] 1.2 扩展 `SysCaseReview`（或列表 VO）承载摘要字段；`SysCaseReviewService.list` 接查询对象
- [x] 1.3 更新 `CaseReviewResource.list` 查询参数与分页响应，保持无新参时行为可用；权限仍 `system:case:query`
- [x] 1.4（可选）补充服务层/Mapper 单测覆盖筛选与聚合

## 2. 前端 API 与路由

- [x] 2.1 更新 `api/system/case-review.js`：`listCaseReviews` 支持筛选与分页参数
- [x] 2.2 在 `router` 为项目 `case` 增加子路由：`case/reviews`、`case/reviews/:reviewId`（静态 `reviews` 优先于通配；对齐 `buildPlanChildren`）
- [x] 2.3 路径工具 / 跳转：从用例页下拉 `push` 到 namespaced `case/reviews`；列表「查看」进详情

## 3. 用例工具栏 split

- [x] 3.1 将 `case/index.vue`「发起评审」改为 `el-dropdown split-button`：主按钮发起（未勾选 disabled）；下拉「发起评审」「评审记录」；箭头与「评审记录」不依赖勾选
- [x] 3.2 补 i18n（评审记录、导出格式等键）；暗色主题下禁用态对比度自检

## 4. 评审记录列表页

- [x] 4.1 新建列表视图：`.list-page-toolbar` 左筛（状态、时间、可选发起人）改值即查；`Cat2BugTable` 列含摘要与操作
- [x] 4.2 操作列：「查看」；已完成「导出」子菜单 md/doc/pdf；`open` 禁用导出
- [x] 4.3 分页与空态（`Cat2BugEmpty`）；权限不足时不暴露发起类入口

## 5. 评审详情页

- [x] 5.1 新建详情视图：人、时间、统计、用例表；`completed` 展示纪要；`open` 提供「继续评审」（复用 `CaseReviewDialog` 或等价）
- [x] 5.2 详情页导出（仅 completed）；返回列表；错误/无权限处理

## 6. 导出实现

- [x] 6.1 md：下载落库 `report_markdown` 快照
- [x] 6.2 doc：Markdown→HTML→`html-docx` 下载
- [x] 6.3 pdf：选定客户端方案（打印预览或轻量转换）并落地；仅 completed

## 7. 回写长期知识

- [x] 7.1 更新 `docs/05-接口文档/接口清单-缺陷与用例计划.md` 中 case-review 列表参数与摘要字段
- [x] 7.2 更新 `docs/03-技术方案/用例评审纪要模板.md` 占位符表与代码一致（`reviewers`/`date`/`caseCount`/`passedCount`/`changesCount` 等）
- [x] 7.3 按需一行更新 `docs/01-产品需求/产品定位与核心功能.md` / 术语表（评审记录页路径）
- [x] 7.4 `docs/08-版本记录` 当月一行记本变更名；无新 ADR / 无新踩坑则注明跳过
