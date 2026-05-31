## Context

缺陷页通过 `Cat2BugStatistic` 横向滚动展示统计块，组件从 `Statistic/*.vue` 自动注册，用户模板存 `sys_user_statistic_template.statistic_templat_config`（`moduleType=1`）。现有块均为文字/进度条（115px 高），无 ECharts。`handle_by` 存于 `sys_defect.handle_by`（JSON 数组）；列表筛选已用 `JSON_CONTAINS`（MySQL）/ `REGEXP`（H2）匹配成员。

项目概览已有 `MemberOfDefectLine`、`DefectStateChart` 等 ECharts 实现，但不在缺陷统计条内，且权限为 `system:dashboard:query`。

## Goals / Non-Goals

**Goals:**

- 提供「未关闭待办负载」REST API（团队 Top N + 个人明细）
- 实现 `MyOpenTodoGauge`、`TeamOpenWorkloadBar` 两个 115px ECharts 统计块
- 点击联动缺陷列表（`parent.search`）
- 无模板的新用户展示默认统计条（含 2 个新块）
- 明暗主题下图表可读

**Non-Goals:**

- 不实现团队类型/状态 ECharts（现有 DefectType/DefectState 保留）
- v1 不迁移 dashboard 的 member-defect-line / defect-line
- v1 不做超期、优先级、驳回率、活跃度块
- 不修改统计条整体高度（保持 115px）

## Decisions

### 1. 待办口径：`handle_by` + 未关闭

- **规则**：`del_flag = '0'` 且 `defect_state != CLOSED`（ordinal 4），且 `handle_by` 包含该 `user_id`
- **多人共管**：同一缺陷对 `handle_by` 中**每个**成员各计 1（产品确认：每人都要跟）
- **个人 API** `/my`：仅统计 `SecurityUtils.getUserId()`；环图可按 `PROCESSING`、`AUDIT`、`REJECTED` 分色（`RESOLVED` 若存在可并入或单独，实现时与 DefectState 展示对齐）

*备选*：用 `sys_user_defect` 关联 — 拒绝，该表侧重收藏/关注，与 `handle_by` 不同步。

### 2. SQL 实现：项目成员 × REGEXP/JSON_CONTAINS

团队 Top N：

```text
sys_user_project up → sys_user u
LEFT JOIN sys_defect d ON d.project_id = up.project_id
  AND d.del_flag = '0'
  AND d.defect_state != 4
  AND handle_by 匹配 u.user_id
GROUP BY u.user_id
ORDER BY total DESC LIMIT 5
```

- **MySQL**：`JSON_CONTAINS(d.handle_by, CAST(u.user_id AS JSON))`（与 `SysDefectMapper` 一致）
- **H2**：`d.handle_by REGEXP CONCAT('\\b', u.user_id, '\\b')`（与现有 H2 缺陷列表一致）

个人 `/my`：对单用户聚合，`SUM(CASE defect_state WHEN ...)` 得各状态计数。

*备选*：Java 层拉全量 open defects 再内存聚合 — 拒绝，项目规模大时浪费带宽。

### 3. API 响应结构（建议）

**团队** `GET /system/defect/statistic/open-workload/{projectId}`

```json
[
  {
    "userId": 1,
    "nickName": "张三",
    "avatar": "...",
    "total": 8,
    "processing": 5,
    "audit": 2,
    "rejected": 1
  }
]
```

**个人** `GET .../open-workload/{projectId}/my`

```json
{
  "total": 12,
  "processing": 7,
  "audit": 3,
  "rejected": 2
}
```

字段名与 `SysDefectStateEnum` 对齐，前端 i18n 用现有 `PROCESSING`/`AUDIT`/`REJECTED` 键。

### 4. 前端 ECharts 块

| 组件 | 图表类型 | 卡片宽度 | 115px 布局要点 |
|------|----------|----------|----------------|
| `MyOpenTodoGauge` | 环形图 `pie` radius `['55%','75%']` | ~200px | 中心显示 `total`；legend 禁用，tooltip 显示状态名 |
| `TeamOpenWorkloadBar` | 横向 `bar` | ~280px | Top5；Y 轴昵称 `truncate`；单系列 `total` 或堆叠 processing/audit |

共用：

- `import * as echarts from '@/assets/js/echarts.min.js'`
- 复用 `@/views/dashboard/mixins/resize`
- `beforeDestroy` 中 `chart.dispose()`
- 颜色：BUG 红 / 验证橙 / 驳回灰蓝等，暗色从 `--text-color-secondary`、`#409eff` 读取

**点击联动**：

```javascript
// 个人 / 团队条
this.parent.search({
  handleBy: [userId],
  params: { defectStates: [0, 1, 3] } // PROCESSING, AUDIT, REJECTED；不含 CLOSED
})
```

与 `DefectState.vue` 的 `parent.search({ params: { defectStates } })` 保持一致。

### 5. 新用户默认模板

在 `Cat2BugStatistic.getStatisticList()` 中：当 `res.rows.length === 0` 时使用内置默认数组（**不强制写库**）；用户首次拖拽/关闭块时再 `addStatistic` 持久化（与现有 `updateStatisticPanel` 行为一致）。

默认顺序：

```json
[
  { "name": "DefectMemberOnline" },
  { "name": "DefectModule" },
  { "name": "DefectState" },
  { "name": "DefectType" },
  { "name": "MyOpenTodoGauge" },
  { "name": "TeamOpenWorkloadBar" },
  { "name": "MyLife" }
]
```

*备选*：后端 insert 默认模板 — v1 用前端 fallback 减少迁移与并发问题。

### 6. CSS 扩展

`Cat2BugStatistic/index.vue` 增加：

```scss
.statistic-item--MyOpenTodoGauge {
  --statistic-item-min-width: 200px;
}
.statistic-item--TeamOpenWorkloadBar {
  --statistic-item-min-width: 280px;
}
```

## Risks / Trade-offs

- **[Risk] 多人共管导致团队 total 之和 > 项目未关闭数** → 产品已接受；UI 标题用「待办负载」而非「缺陷总数」
- **[Risk] `handle_by` 为空或 malformed JSON** → SQL 条件排除；计数为 0
- **[Risk] 115px 内 ECharts 轴标签拥挤** → Top5 + 昵称截断 + 隐藏 x 轴刻度
- **[Risk] 暗色主题对比不足** → 使用 `--cat2bug-table-sort-caret-active` 等同系变量做 spot check
- **[Trade-off] 默认 7 块统计条较宽** → 保留横向滚动与左右箭头（已有）

## Migration Plan

- 纯增量：无 DB 迁移；老用户模板不变，需手动在 StatisticTemplate 添加新块
- 部署：先后端再前端；回滚删除新接口与组件即可

## Open Questions

- （v1 无）v2 是否将 `member-activity-line` 迁入 defect statistic
