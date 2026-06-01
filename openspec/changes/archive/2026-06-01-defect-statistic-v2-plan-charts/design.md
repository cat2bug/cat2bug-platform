## Context

缺陷页 `Cat2BugStatistic` 统计块高度固定 **115px**（`--statistic-card-height`）。v1 已归档：待办负载 API、`MyOpenTodoGauge`、`TeamOpenWorkloadBar`，默认模板 7 块。模版页已支持 `template-group="personal|team"` 分组展示。

仪表盘 `PlanBurndownChart` 使用 `dashboardPlanList` + `planBurndown`（按日累计 `sys_plan_item` 通过数），权限 `system:dashboard:query`。`PlanStatisticsChart` / `planStatisticsExport` 从 `SysPlan` 计算发现率、修复率等字符串百分比。

产品决策（已确认）：

1. 个人参与口径：**缺陷日志**（`sys_defect_log`，`create_by = 当前用户`）
2. 统计块高度：**必须 115px**
3. 默认模板：**不增加**新块，仅模版自选
4. 雷达轴：**发现率、修复率、探测率、严重率、重开率、逃逸率**（6 项，与 `SysPlan` getter 一致）

## Goals / Non-Goals

**Goals:**

- 缺陷统计域提供参与热力、计划燃尽、计划指标三类 API
- 三个 115px ECharts 统计块，注册到个人/团队模版分组
- 热力图点击联动缺陷列表（日志参与日）
- 明暗主题可读；`refreshData` 可刷新

**Non-Goals:**

- 不修改 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`（仍为 v1 的 7 块）
- 不做全年 GitHub 式日历网格
- 雷达不展示缺陷密度、平均修复时长
- 不要求用户具备仪表盘权限
- v2 不迁移 `MemberOfDefectLine` 等其它 dashboard 图表

## Decisions

### 1. 个人参与：日志按日 distinct defect_id

**规则：**

```sql
SELECT date_format(l.create_time, '%Y-%m-%d') AS day,
       COUNT(DISTINCT l.defect_id) AS cnt
FROM sys_defect_log l
JOIN sys_defect d ON d.defect_id = l.defect_id
WHERE d.project_id = #{projectId}
  AND d.del_flag = '0'
  AND l.create_by = #{userId}
  AND l.create_time >= #{startDate}
GROUP BY day
```

- 默认窗口 **30 天**（查询参数 `days`，允许 14–90，实现时 clamp）
- 无记录日期 **补 0**（与 dashboard 燃尽日期补齐同理，在 Service 层）
- H2 使用 `FORMATDATETIME` / `PARSEDATETIME` 双实现

*备选*：按缺陷 `update_time` —— 拒绝，与「按日志」产品定义不符。

### 2. 热力图 UI：单行 30 日热力条（非年历）

115px 卡片内可用图表高度约 **70px**（`Cat2BugCard` 头区 + padding）。采用 ECharts `heatmap`：

- `y` 轴固定 1 行（`category: ['']`）
- `x` 轴为最近 N 天日期（标签隐藏，tooltip 显示 `日期 + 次数`）
- `visualMap: false`，颜色阶梯 4 档（0 / 低 / 中 / 高）
- 块宽度 `--statistic-item-min-width: 300px`

### 3. 热力图点击联动

缺陷列表新增筛选（`SysDefectMapper`）：

```xml
<if test="defect.params.participationLogDate != null">
  AND EXISTS (
    SELECT 1 FROM sys_defect_log l
    WHERE l.defect_id = d.defect_id
      AND l.create_by = #{defect.params.participationUserId}
      AND date(l.create_time) = #{defect.params.participationLogDate}
  )
</if>
```

前端点击：

```javascript
this.parent.search({
  params: {
    participationLogDate: '2026-06-01',
    participationUserId: currentUserId
  }
})
```

`read` 模式不触发。若无日志记录日，单元格不可点或点击无操作。

### 4. 测试计划燃尽：缺陷域代理 + 迷你图

- Controller 调用已有 `sysDashboardService.planBurndown(planId)`，并复用 `SysDashboardController.planBurndown` 的**日期轴补齐**逻辑（提取为 package-private 工具或 Service 方法，避免重复）
- 计划列表：`selectSysPlanList` where `projectId`，返回 `planId`、`planName`，默认选第一条
- UI：`TeamPlanBurndown.vue` 参考 `PlanBurndownChart.vue` 的下拉 + 标题前缀，但：
  - 外层 `Cat2BugCard`
  - `grid` 极简，`xAxis`/`yAxis` 标签 `show: false`
  - 柱形 `type: 'bar'`，高度 ~60px
  - `min-width: 320px`

### 5. 测试计划雷达：6 轴数值化 + 展示 Top N

**指标**（与 `SysPlan` 一致，Service 转为 `double` 0–100）：

| 字段 | 来源 |
|------|------|
| discovery | `getDefectDiscoveryRate()` 去 `%` |
| repair | `getDefectRepairRate()` |
| detection | `getDefectDetectionRate()` |
| severity | `getDefectSeverityRate()` |
| restart | `getDefectRestartRate()` |
| escape | `getDefectEscapeRate()` |

API 返回项目**全部**计划数组；前端雷达：

- `indicator`: 6 轴，`max: 100`，`axisName.show: false`（tooltip 显示全名）
- `series`: 每计划一条 `radar` 多边形；当计划数 **> 5** 时仅绘制 **按 `update_time` 降序 Top 5**，tooltip/副标题注明「共 N 个计划」
- `min-width: 320px`

*备选*：每计划独立雷达 —— 拒绝，115px 无法容纳。

### 6. 模版注册，不改默认模板

```javascript
export const PERSONAL_STATISTIC_NAMES = [
  'MyOpenTodoGauge', 'MyLife', 'MyParticipationHeatmap'
]
export const TEAM_STATISTIC_NAMES = [
  'DefectMemberOnline', 'DefectModule', 'DefectState', 'DefectType',
  'TeamOpenWorkloadBar', 'TeamPlanBurndown', 'TeamPlanMetricsRadar'
]
```

`DEFAULT_DEFECT_STATISTIC_TEMPLATE` **保持不变**。

### 7. 权限与依赖

- 所有新接口：`@PreAuthorize("@ss.hasPermi('system:defect:query')")`
- 复用 `echarts.min.js`、`resize` mixin、`Cat2BugCard`
- `Cat2BugStatistic.refreshData` 增加对 `getParticipation`、`getPlanBurndown`、`getPlanMetrics` 等方法名的调用

## Risks / Trade-offs

- **[Risk] 115px 雷达可读性** → Top 5 绘制 + tooltip；轴名隐藏
- **[Risk] 热力条在窄宽度下格子过密** → min-width 300px；可选仅显示 14 天（`days` 参数）
- **[Risk] 燃尽日期补齐逻辑重复** → 抽取共享方法，单测补齐边界
- **[Risk] 计划数很多时 API payload** → 仅返回雷达所需字段，不含 `sysPlanItemList`
- **[Trade-off] 热力图非经典年历** → 产品已接受 30 日条带

## Migration Plan

- 纯增量，无 DB 表结构变更（仅 Mapper 条件）
- 部署：先后端再前端
- 回滚：删除新接口与 3 个组件即可

## Open Questions

- （无阻塞项）若后续需要仪表盘权限与缺陷统计 API 合并，可单独 refactor
