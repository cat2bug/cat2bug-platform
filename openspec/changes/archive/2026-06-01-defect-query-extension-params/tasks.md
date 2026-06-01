## 1. 查询扩展工具

- [x] 1.1 新增 `DEFECT_QUERY_EXTENSION_KEYS` 与 `clearExtensionParams(queryParams)`（当前仅 participation 两键）
- [x] 1.2 为 `clearExtension` 补充简短注释或 JSDoc，说明新增扩展域的注册方式

## 2. defect/index.vue 协调 API

- [x] 2.1 实现 `searchQuery({ common, extension, stack })`，行为符合 design.md 决策表
- [x] 2.2 `stack: false` 时：`reset()`、参与路径强制 `activeDefectTabName = 全部`、写 extension/common
- [x] 2.3 实现 `resetQueryByCurrentTab()`：`clearExtension` + 复用当前 Tab 基准逻辑
- [x] 2.4 在 `selectDefectTabHandle` 各分支进入查询前调用 `clearExtensionParams()`
- [x] 2.5 工具栏无独立「重置」按钮；类型下拉主按钮已绑定 `selectDefectTabHandle`（含 clearExtension），`resetQueryByCurrentTab` 供后续入口复用
- [x] 2.6 `searchByParticipation` 改为调用 `searchQuery`（或删除并改调用方）
- [x] 2.7 （可选）`search(params)` 委托 `searchQuery({ common: params })` 并标 deprecated

## 3. 统计条迁移

- [x] 3.1 `Cat2BugStatistic` 增加 `searchQuery` 转发
- [x] 3.2 `MyParticipationHeatmap` 点击改用 `searchQuery({ stack: false, extension, common })`
- [x] 3.3 其它 Statistic 块（待办、状态、模块、成员在线等）改为 `searchQuery({ common })` 或经薄封装 `search`

## 4. 验证

- [x] 4.1 手工：参与某日筛选 → 点「我的待办」→ 列表无参与日条件
- [x] 4.2 手工：参与筛选 → 点「重置」→ 当前 Tab 基准且无参与条件
- [x] 4.3 手工：参与筛选 → 切自定义 Tab → 无参与残留（或符合 Tab config）
- [x] 4.4 补充 `TESTING.md` 三条路径（可选）
