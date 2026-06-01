## Context

缺陷页 `Cat2BugStatistic` 统计块高度固定 **115px**（`--statistic-card-height`）。v1 默认 7 块；v2 已在模版自选区提供 `MyParticipationHeatmap`、`TeamPlanBurndown`、`TeamPlanMetricsRadar`。

`MyLife` 已验证：点击卡片 → `el-dialog` 配置 → 写入模版 `params` 或用户配置。`TeamPlanBurndown` 已验证：`statisticPlanList` + `params.planId` 记忆选中计划。

`SysPlan` 字段：`planEndTime`、`passCount`、`unexecutedCount`、`defectCount`、`defectCloseStateCount`（`getExecutedCount()` = `itemTotal - unexecutedCount`，**本变更不使用**作为「已完成」展示）。

## 产品决策（已确认）

| 项 | 决策 |
|----|------|
| 报时生效范围 | 仅缺陷统计条打开（组件 mounted 且非 read） |
| 已完成用例 | **仅** `passCount`，不含 `failCount` |
| 报时条数 | **一个模块、多条** `timers[]` |
| 音效 | 接受打包小体积静态 mp3/ogg |
| 配置入口 | **点击卡片** 打开 dialog（对齐 `MyLife`） |
| 每月 31 日 | 无 31 日的月份 → 该月 **最后一天** 触发 |
| 周期规则 | `daily`/`weekly`/`monthly` **循环**；`once` **仅响一次** 后标为已触发或禁用 |

## Goals / Non-Goals

**Goals:**

- 两个 115px 统计块，注册个人/团队模版分组
- 个人报时：多条、倒计时展示、到点音效、对话框 CRUD
- 团队倒计时：自然日差、逾期红色、四指标、计划下拉
- 7 语言 i18n；明暗主题可读

**Non-Goals:**

- 不修改 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`
- 不做 `Notification` API / 桌面通知 / 页签隐藏仍报时
- 不把 `pass+fail` 或 `executedCount` 当作「已完成用例」
- 不新增后端表；默认不新增 REST（除非 `getPlan` 权限阻塞）

## Decisions

### 1. 个人报时：纯前端调度

**组件生命周期：**

```
mounted + !read + 有 timers
  → startTick(1s)：更新各条 nextFireAt、countdown 文案
  → 到点：play(soundId)，标记 lastFiredAt；once 类型 disable
beforeDestroy / deactivated（若用 keep-alive）
  → clearInterval，暂停 Audio
```

**不**在 `document.hidden` 时继续报时（可选：隐藏时暂停 tick，显示时重算 next——实现采用 **隐藏时暂停调度**，与「仅统计条打开」一致）。

**read 模式**（`StatisticTemplate` 预览）：显示静态倒计时样例或「预览不播报」，**禁止** `Audio.play`。

### 2. timers 数据模型（模版 params）

```json
{
  "name": "PersonalRemindTimer",
  "params": {
    "timers": [
      {
        "id": "uuid-v4",
        "label": "站会",
        "enabled": true,
        "sound": "bell-soft",
        "schedule": {
          "type": "once",
          "date": "2026-06-15",
          "time": "09:00"
        }
      },
      {
        "id": "uuid-v4-2",
        "label": "周报",
        "enabled": true,
        "sound": "chime",
        "schedule": {
          "type": "weekly",
          "weekday": 5,
          "time": "18:00"
        }
      }
    ]
  }
}
```

| type | 字段 | next 计算 |
|------|------|-----------|
| `once` | `date` (YYYY-MM-DD), `time` (HH:mm) | 若已过且未触发 → 不再触发 |
| `daily` | `time` | 今日该时刻或明日 |
| `weekly` | `weekday` (1=周一…7=周日), `time` | 本周或下周最近一刻 |
| `monthly` | `day` (1–31), `time` | 本月有效日或下月；**day>月末→月末** |

持久化：通过统计模版保存 API（与现有模版项 `params` 一致），**不**写入 `user-config`（与 `MyLife` 区分，避免跨项目串配置）。

### 3. 115px 块内 UI（PersonalRemindTimer）

- 标题：`defect.personal-remind-timer`
- 内容区：最多展示 **2 行** 最近即将触发的规则（按 `nextFireAt` 升序）；超出显示「+N」或 `overflow-y: auto`（max-height ~70px）
- 每行：`HH:mm 标签` + `countdown`（`HH:mm:ss` 或 `Nd HH:mm:ss`）
- **倒计时数字** MUST 使用电子管风格字体（`Nixie One`，类名 `.statistic-countdown-nixie`），见 §8
- 整块 `cursor: pointer`，`@click` 打开 dialog（工具按钮若存在，不冒泡冲突）
- Dialog：表格/列表 — 启用开关、标签、类型、时间、铃声、试播、增删
- 音效文件：`cat2bug-platform-ui/public/sounds/` 或 `src/assets/sounds/`（构建可复制），建议 ≤50KB/个，映射常量 `SOUND_OPTIONS`

```javascript
export const PERSONAL_STATISTIC_NAMES = [
  'MyOpenTodoGauge', 'MyLife', 'MyParticipationHeatmap', 'PersonalRemindTimer'
]
```

`min-width: 280px`（与 `MyLife` 接近）。

### 4. 团队计划倒计时（TeamPlanCountdown）

**主倒计时展示：**

- 剩余/逾期天数数字 MUST 使用 `.statistic-countdown-nixie`（电子管字体 + 暖色辉光；逾期时叠加 `--overdue` 红色辉光）
- 副文案（「天」「逾期」等）可用常规 UI 字号，不必强制 Nixie

**天数计算（自然日，本地时区）：**

```javascript
const end = startOfDay(parse(planEndTime))
const today = startOfDay(new Date())
const diffDays = differenceInCalendarDays(end, today)
// diffDays > 0 → 剩余 diffDays 天
// diffDays === 0 → 今天结束（文案「今天」或「剩余 0 天」产品统一为「今天」）
// diffDays < 0 → 逾期 Math.abs(diffDays) 天，class overdue（红色）
```

**指标展示（单行或 2×2 紧凑）：**

| 文案键 | 值 |
|--------|-----|
| 未完成用例 | `unexecutedCount` |
| 已完成用例 | `passCount` |
| 未解决缺陷 | `defectCount - defectCloseStateCount` |
| 已解决缺陷 | `defectCloseStateCount` |

**数据加载：**

1. `statisticPlanList(projectId)` — 与燃尽块相同
2. `getPlan(planId)` — 读取计数与 `planEndTime`
3. `params.planId` 记忆上次选择；列表变更时校验 id 仍存在

**权限：** 若缺陷页用户仅有 `system:defect:query` 而无 `system:plan:query` 导致 `getPlan` 403，则 **fallback**：在 `SysDefectStatisticController` 增加 `GET /system/defect/statistic/plan/{planId}/summary` 返回上述字段子集（实现任务中按实测决定，design 优先尝试 `getPlan`）。

```javascript
export const TEAM_STATISTIC_NAMES = [
  ..., 'TeamPlanBurndown', 'TeamPlanMetricsRadar', 'TeamPlanCountdown'
]
```

`min-width: 320px`。

### 5. 模版注册

- **不**修改 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`
- `Cat2BugStatistic.refreshData`：`TeamPlanCountdown` 重新 `loadPlan`；`PersonalRemindTimer` 仅重算 countdown（无 API）

### 6. 浏览器与音频限制

- 首次用户交互前浏览器可能阻止自动播放：到点播放失败时 **静默** 或 `console.debug`，不在 dialog 外弹错
- Dialog 内「试播」按钮由用户点击触发，用于解锁音频策略

## Risks / Trade-offs

- **[Risk] 115px 多条倒计时可读性** → 仅显示最近 2 条 + 滚动/「+N」
- **[Risk] 页签后台计时漂移** → 隐藏页签暂停 tick；回到前台重算 `nextFireAt`
- **[Risk] getPlan 权限** → 必要时增加 defect 域 summary 代理
- **[Trade-off] 无系统通知** → 用户需保持缺陷页打开；产品已接受

## Migration Plan

- 纯前端增量 + 可选一个只读 summary API
- 部署：先后端（若有 summary）再前端
- 回滚：移除两组件与注册即可

### 8. 电子管倒计时字体（Nixie One）

- 字体：**Nixie One**（SIL OFFL 1.1），通过 `@fontsource/nixie-one` 自托管打包，避免 CDN 依赖
- 全局样式：`src/assets/styles/statistic-countdown-nixie.scss`
- 用法：倒计时主数字包裹 `<span class="statistic-countdown-nixie">`；逾期加 `statistic-countdown-nixie--overdue`
- 浅色主题：暖琥珀色 `#c45c26` + 轻辉光；暗色：`#ff9a3c` + 较强 `text-shadow` 模拟辉光管
- `font-variant-numeric: tabular-nums` 保证数字等宽跳动稳定

## Open Questions

- （无阻塞）`getPlan` 权限若已满足缺陷页角色，则不新增 summary API
