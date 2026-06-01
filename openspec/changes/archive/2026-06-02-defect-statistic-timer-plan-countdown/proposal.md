## Why

缺陷页统计条 v2 已提供参与热力、计划燃尽与雷达，但缺少两类轻量信息块：

1. **个人报时**：测试/站会等需在固定时刻提醒，且应仅在用户查看缺陷统计时生效，避免全局后台打扰。
2. **团队计划倒计时**：测试负责人需在 115px 内一眼看到距计划结束的天数、用例与缺陷完成口径，并在逾期时醒目提示。

二者均属于统计模版自选能力，不应改动默认 7 块模板，也不依赖仪表盘权限。

## What Changes

- 新增个人统计块 **`PersonalRemindTimer`**：
  - 单块内支持 **多条** 报时规则（`once` / `daily` / `weekly` / `monthly`）
  - 仅在缺陷页统计条 **挂载且非 read** 时调度；离开页面或销毁组件停止报时
  - 块内展示距下次触发的倒计时（按最近一条优先，多条可滚动）
  - **点击卡片** 打开配置对话框（交互参考 `MyLife`）
  - 内置 3～5 个小体积音效（静态资源），到点 `Audio` 播放
  - 配置持久化在统计模版项 `params.timers[]`（随项目统计模板保存）
- 新增团队统计块 **`TeamPlanCountdown`**：
  - 相对 `planEndTime` 的 **自然日** 倒计时；逾期显示「逾期 N 天」且为红色
  - 指标：**未完成用例** `unexecutedCount`；**已完成用例** 仅 `passCount`（不含 fail）；**未解决缺陷** `defectCount - defectCloseStateCount`；**已解决缺陷** `defectCloseStateCount`
  - 计划下拉与 `TeamPlanBurndown` 一致（`statisticPlanList` + `params.planId`）
  - 数据优先复用 `GET /system/plan/{planId}`（`getPlan`）；可选后续瘦身 API
- 注册到 `PERSONAL_STATISTIC_NAMES` / `TEAM_STATISTIC_NAMES`，**不**写入 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`
- i18n 7 语言；`StatisticTemplate` 预览区可见

## Capabilities

### New Capabilities

- `personal-remind-timer-statistic`：个人多条报时、前端调度、音效、对话框配置、模版 params 持久化
- `team-plan-countdown-statistic`：团队计划日倒计时、四指标口径、计划切换、115px 展示

### Modified Capabilities

- （无）不修改既有 `defect-plan-statistic-charts` 等规格行为

## Impact

- **前端**：新增 `Statistic/PersonalRemindTimer.vue`、`Statistic/TeamPlanCountdown.vue`；`Cat2BugStatistic/index.vue` 注册与 CSS 宽度；`StatisticTemplate.vue` 分组；`assets/sounds/` 静态音效；i18n 7 文件
- **后端**：默认 **无** 新接口（报时纯前端；倒计时用现有 `getPlan`）。若 `getPlan` 权限为 `system:plan:query` 且与缺陷页不一致，需在 design 中评估是否增加缺陷域 summary 代理（见 design Open Questions 已闭合：优先 `getPlan`）
- **Non-Goals**：不做浏览器系统通知、不做 Service Worker、不提高统计条高度、不将 fail 计入「已完成用例」、不自动向老用户模板插入新块
