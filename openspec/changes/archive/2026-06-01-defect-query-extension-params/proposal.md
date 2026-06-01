## Why

缺陷页列表与统计条共用 `queryParams`，通用条件（类型、处理人、`params.defectStates` 等）通过 `search()` 深度合并。专用条件（当前仅「我的参与」的 `participationLogDate` / `participationUserId`）若未显式清除，会在用户点击其它统计块后残留在请求中，导致列表筛选错误。

「我的参与」已实现 `searchByParticipation`（`reset` + 切「全部」Tab），但与通用 `search()` 两套入口，难以扩展且易遗漏清空逻辑。

## What Changes

- 在 `defect/index.vue` 引入统一查询入口 **`searchQuery({ common, extension, stack })`**（方案 B）：
  - `common`：与现 `search(params)` 相同语义的通用条件（merge）
  - `extension`：非共享参数对象（当前仅参与两字段）
  - `stack`：默认 `true`（可叠加）；**我的参与** 必须 `stack: false`（不叠加，整页条件重建）
- 抽取 **`clearExtensionParams()`**，从 `queryParams.params` 删除已注册的扩展键
- **`resetQueryByCurrentTab()`**：列表「重置」与 Tab 切换语义一致——按当前激活 Tab 恢复基准 `queryParams`，并 **`clearExtensionParams()`**
- `Cat2BugStatistic` 转发 `searchQuery`；`MyParticipationHeatmap` 使用 `stack: false` + `extension`；其它统计块仅传 `common`（隐式清扩展）
- 保留 `searchByParticipation` 为薄封装（可选，内部仅调 `searchQuery`）或迁移后删除
- **后端不变**：仍使用 `defect.params.participation*`

## Capabilities

### New Capabilities

- `defect-list-query-extension`：缺陷列表扩展查询参数生命周期（`searchQuery` / `clearExtension` / 按 Tab 重置）

### Modified Capabilities

- `defect-participation-heatmap`：点击联动改为通过 `searchQuery` 且 `stack: false`、强制「全部」Tab

## Impact

- **前端**：`views/system/defect/index.vue`、`components/Cat2BugStatistic/index.vue`、`Statistic/MyParticipationHeatmap.vue`；各统计块 `parent.search` 逐步改为 `parent.searchQuery`
- **测试**：可选单测/手工用例——参与筛选后点「我的待办」应无参与条件；重置后回到当前 Tab 基准
- **Non-Goals**：不拆分后端 DTO；不新增第二套扩展域（日历等仍由视图自行管理，本变更仅注册参与键）；不改变参与 API 与热力图 UI
