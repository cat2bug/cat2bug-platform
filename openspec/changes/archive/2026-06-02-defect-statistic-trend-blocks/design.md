## Context

缺陷页 `Cat2BugStatistic` 统计块高度 **115px**。团队图块 v2 已具备 `TeamPlanBurndown`（320px 宽、紧凑 ECharts、`echarts-resize` + `chart-theme`）与缺陷域 API 代理模式（`openspec/specs/defect-plan-statistic-charts`）。

仪表盘实现：

| 仪表盘 | 组件 | API | 数据口径 |
|--------|------|-----|----------|
| 缺陷状态走势 | `DefectStateChart.vue` | `GET /system/dashboard/{projectId}/defect-line` | `sys_defect` 按 `update_time` 日/月 × `defect_state` 计数 |
| 成员处理走势 | `MemberOfDefectLine.vue` | `GET /system/dashboard/{projectId}/member-defect-line` | `sys_defect_log` 按 `create_by` + 日/月 计数 |

统计条已有 `DefectState` 为**当前快照**标签块，与走势块职责互补。

## 产品决策（已确认）

| 项 | 决策 |
|----|------|
| 块宽度 | 与燃尽图相同 **`min-width: 320px`** |
| 成员系列 | **全员**，不 Top N |
| 列表联动 | **需要**；`read` 模式不触发 |
| 默认模板 | **不**加入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE` |

## Goals / Non-Goals

**Goals:**

- 两个 115px 团队自选块，交互与仪表盘一致（时间粒度切换 + 导出）
- 缺陷域 API + `system:defect:query`
- 点击联动缺陷列表（状态+日期 / 成员+参与日）
- 7 语言 i18n；明暗主题

**Non-Goals:**

- 不修改仪表盘页面与 `system:dashboard:query` 路由
- 不自动向老用户模板插入新块
- 不做个人版走势块（本变更仅团队模版）

## Decisions

### 1. API：缺陷统计域代理

在 `SysDefectStatisticController` 增加：

```
GET  /system/defect/statistic/defect-state-line/{projectId}?timeType=day|month
GET  /system/defect/statistic/member-defect-line/{projectId}?timeType=day|month
POST /system/defect/statistic/defect-state-line/{projectId}/export?timeType=...
POST /system/defect/statistic/member-defect-line/{projectId}/export?timeType=...
```

实现委托 `ISysDashboardService.defectLine` / `memberOfDefectsLine`；export 逻辑与 `SysDashboardController` 对应 export **同结构**（可抽取 package-private 工具或 Service 方法复用，避免复制漂移）。

权限：`@PreAuthorize("system:defect:query")`。

**响应形状**与仪表盘保持一致，便于前端复用转换逻辑：

- 状态走势：`{ data: Map<stateName, List<count>>, types: SysDefectStateEnum[], times: string[] }`
- 成员走势：`{ time: string[], data: SysMemberOfDefectsLine[] }`，且每条 `data` **新增 `userId`（Long）**

### 2. 成员走势：SQL 与领域对象扩展

`SysDashboardMapper.memberOfDefectsLine` 子查询已有 `l.create_by AS update_by_id`，但 resultMap 未映射。变更：

- `SysMemberOfDefectsLine.userId`
- resultMap 增加 `userId` ← `update_by_id`
- H2 / MySQL 两份 SQL 均 SELECT 该列（若尚未透出）

缺陷统计域 API 与仪表盘 API **共用** Mapper；仪表盘 JSON 多字段 `userId` 对旧前端无破坏。

### 3. 前端组件结构

```
Statistic/
  TeamDefectStateTrend.vue      # Cat2BugCard + header 工具区
  TeamMemberDefectTrend.vue
utils/
  defect-trend-chart.js         # buildDefectStateLineOption / buildMemberDefectLineOption(compact)
  (可选) 从 dashboard 组件迁入 build 函数，dashboard 改 import
```

**布局（对齐 TeamPlanBurndown）：**

- 标题：i18n `defect.team-defect-state-trend` / `defect.team-member-defect-trend`
- `left-tools` 或 `right-tools`：`el-radio-group` 30天/12月 + 导出按钮
- 内容区：单 ref 画布，`height: 100%`，grid `top` 压缩（compact 约 8–12px，无 dashboard 的 top:70）

**图例：** `type: 'scroll'`，`top: 0` 或隐藏图例仅依赖 tooltip（实现时二选一，优先 scroll 以支持全员辨认系列色）。

**注册：**

```javascript
export const TEAM_STATISTIC_NAMES = [
  // ...existing,
  'TeamDefectStateTrend',
  'TeamMemberDefectTrend'
]
```

```scss
.statistic-item--TeamDefectStateTrend,
.statistic-item--TeamMemberDefectTrend {
  --statistic-item-min-width: 320px;
}
```

`refreshData()` → 重新请求当前 `timeType` 数据。

### 4. 列表联动

#### TeamDefectStateTrend

ECharts `click` 事件解析 `seriesName`（状态枚举名，如 `PROCESSING`）与 `name`（x 轴日期 `yyyy-MM-dd` 或 `yyyy-MM`）。

映射状态名 → `defectStates` 逗号分隔 ID（与 `DefectState.vue` / `SysDefectStateEnum` 序一致）：

| 枚举名 | defectStates（示例，以实现为准） |
|--------|----------------------------------|
| PROCESSING | `0` |
| AUDIT | `1` |
| RESOLVED | `2` |
| REJECTED | `3` |
| CLOSED | `4` |

调用：

```javascript
parent.searchQuery({
  stack: false,
  common: {
    params: {
      defectStates: [stateId],  // 或逗号串，与列表现有格式一致
      beginUpdateTime: dayStart,
      endUpdateTime: dayEnd,
      delFlag: '0'
    }
  }
})
```

- `timeType=day`：`beginUpdateTime` = 当日 `00:00:00`，`endUpdateTime` = 当日 `23:59:59`（本地时区格式与列表现有一致）
- `timeType=month`：该月首日 00:00:00 至月末 23:59:59

`read === true` 时 return。

列表 Mapper 已支持 `params.beginUpdateTime` / `endUpdateTime`（`SysDefectMapper.xml`），**无需新扩展键**。

#### TeamMemberDefectTrend

点击解析 `userId`（从 series 元数据或 side cache）与 x 轴日期：

```javascript
parent.searchQuery({
  stack: false,
  extension: {
    participationLogDate: clickedDate,  // yyyy-MM-dd；月模式用 yyyy-MM-01 或文档约定
    participationUserId: userId
  },
  common: {
    params: { defectStates: [], delFlag: '0' }
  }
})
```

与 `searchByParticipation` / `MyParticipationHeatmap` 一致；`stack: false` 避免与其它筛选叠加歧义（与热力图一致）。

**月模式：** `participationLogDate` 仅支持按日筛选时，月视图点击宜传该月**点击点对应月份内用户有日志的缺陷**——v1 采用点击具体 x 标签日期（月模式下 x 为 `yyyy-MM`，Mapper 需支持按月或实现改为传该月任意代表日 + 文档说明）。**实现建议：** 月模式 x 轴值为 `yyyy-MM` 时，扩展筛选使用 `beginUpdateTime`/`endUpdateTime` 覆盖整月 **且** `participationUserId`；若仅 participation 扩展支持按日，则在 design 任务中增加 Mapper 条件 `DATE_FORMAT(l.create_time,'%Y-%m') = #{month}` 或 participation 扩展键 `participationLogMonth`。为降低范围，**v1 月模式点击**：`participationLogDate` 传该月第一天，`participationUserId` + 文档注明「月视图按当月参与」需在 Mapper 增加 `participationLogMonth` 可选键。

**简化 v1 闭合：** 月模式点击使用 `extension.participationLogMonth`（`yyyy-MM`）+ `participationUserId`，Mapper 增加对应条件（与 participation 扩展注册表一并登记）。日模式仍用 `participationLogDate`。

### 5. 导出

统计块导出调用缺陷域 `POST .../export`，文件名 i18n 标题 + 时间戳；`read` 模式隐藏或禁用导出按钮。

### 6. 与 DefectState 块的区别

| 块 | 类型 | 联动 |
|----|------|------|
| DefectState | 快照 + 日/周增量 | 仅状态 |
| TeamDefectStateTrend | 时间序列 | 状态 + update 日/月 |

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 全员折线在 320px 内拥挤 | legend scroll、细线、tooltip |
| 月模式参与日筛选 | 新增 `participationLogMonth` 扩展键 |
| export 代码重复 | 抽取 Dashboard 与 Statistic 共用导出构建 |

## Open Questions

- （无）产品三项已确认。
