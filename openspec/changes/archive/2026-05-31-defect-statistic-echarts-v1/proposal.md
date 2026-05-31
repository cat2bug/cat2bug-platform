## Why

缺陷页统计条已有团队「状态 / 类型 / 交付物 / 成员在线」等文字列表块，但缺少**个人待办**与**团队负载均衡**的可视化；项目概览页的 ECharts 图表未接入缺陷页，且依赖 `system:dashboard:query` 权限。v1 在 115px 统计条内新增 ECharts 块，并基于 `handle_by + 未关闭` 口径提供待办负载 API，让个人与项目管理者一眼看到「谁手上还有单」。

## What Changes

- 新增后端接口（挂载 `/system/defect/statistic/`）：
  - `GET .../open-workload/{projectId}` — 团队各成员未关闭待办数（Top N）
  - `GET .../open-workload/{projectId}/my` — 当前用户未关闭待办及按状态拆分
- 待办口径：`del_flag = '0'`、`defect_state != CLOSED`，且 `handle_by` JSON 数组**包含**该成员；多人共管时**每人各计一次**
- 新增前端统计块（`Cat2BugStatistic/Statistic/`）：
  - `MyOpenTodoGauge` — 个人环形图（115px）
  - `TeamOpenWorkloadBar` — 团队横向条形 Top5（115px）
- 点击图表扇区/条目标记联动缺陷列表筛选（`handleBy` + 未关闭状态）
- 新用户（无统计模板记录）使用默认模板，包含现有 4 块 + MyLife + 上述 2 个 ECharts 块
- ECharts 主题适配明暗色（CSS 变量 / `theme-dark`）
- **Non-Goals（v1 不做）**：团队类型/状态 ECharts 版、超期/优先级/活跃度块、dashboard 接口迁移

## Capabilities

### New Capabilities

- `defect-open-workload-statistic`：未关闭待办负载统计 API（团队 + 个人），含多人共管计数规则与 H2/MySQL 双实现
- `defect-statistic-echarts-blocks`：缺陷页 ECharts 统计块、默认模板、列表联动与 115px 布局约定

### Modified Capabilities

（无：现有 `openspec/specs/` 中无缺陷统计条 capability）

## Impact

- **后端**：`SysDefectStatisticController`、`ISysDefectStatisticService`、`SysDefectStatisticMapper`（+ XML H2/MySQL）
- **前端**：`Cat2BugStatistic/index.vue`（默认模板、块宽度 CSS）、新增 2 个 Statistic 组件、`api/system/statistic/defect.js`、i18n、`theme-dark.scss`（图表色）
- **依赖**：已有 `@/assets/js/echarts.min.js`、dashboard `resize` mixin 可复用
- **权限**：`system:defect:query`（与现有缺陷统计一致）
- **用户可见**：缺陷页统计条可添加/默认展示「我的待办」「团队待办负载」；不影响现有 DefectState/DefectType 列表块
