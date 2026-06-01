## 1. 前端：音效、字体与常量

- [x] 1.1 依赖 `@fontsource/nixie-one`；`statistic-countdown-nixie.scss` 在 `index.scss` 引入；类 `.statistic-countdown-nixie` / `--overdue`
- [x] 1.2 在 `public/sounds/`（或 `assets/sounds/`）添加 ≥3 个短音效（bell-soft、chime、tick 等），单文件 &lt;50KB
- [x] 1.3 新增 `Statistic/utils/remind-timer.js`：`SOUND_OPTIONS`、`computeNextFireAt(schedule, now)`、`formatCountdown(ms)`、月末 day 兜底

## 2. 前端：PersonalRemindTimer

- [x] 2.1 新建 `Statistic/PersonalRemindTimer.vue`：卡片倒计时列表（最近 2 条 + 溢出处理）、115px；倒计时数字用 `.statistic-countdown-nixie`
- [x] 2.2 实现配置 `el-dialog`：增删改、`enabled`、`schedule` 类型表单、音效试播
- [x] 2.3 实现调度：`mounted` 启动 1s tick，`beforeDestroy` 清理；`document.hidden` 暂停；`read` 不播报
- [x] 2.4 到点 `Audio` 播放；`once` 触发后禁用；`params.timers` 与模版保存联动（`@update:params` 或现有模版保存路径）
- [x] 2.5 点击卡片打开 dialog（对齐 `MyLife` 交互）

## 3. 前端：TeamPlanCountdown

- [x] 3.1 新建 `Statistic/TeamPlanCountdown.vue`：计划下拉（复用 `statisticPlanList`）、自然日倒计时（Nixie 主数字 + 逾期 `--overdue`）
- [x] 3.2 调用 `getPlan(planId)` 展示四指标（`unexecutedCount`、`passCount`、未解决/已解决缺陷）；无 `planEndTime` 空态
- [x] 3.3 若 `getPlan` 403：实现 `GET /system/defect/statistic/plan/{planId}/summary` 后端 + 前端 fallback（仅在此情况）
- [x] 3.4 实现 `refreshData()`；`params.planId` 记忆

## 4. 前端：统计条集成

- [x] 4.1 更新 `PERSONAL_STATISTIC_NAMES`、`TEAM_STATISTIC_NAMES`（不改 `DEFAULT_DEFECT_STATISTIC_TEMPLATE`）
- [x] 4.2 `index.vue` 动态组件映射 + `.statistic-item--PersonalRemindTimer`、`.statistic-item--TeamPlanCountdown` 宽度 CSS
- [x] 4.3 `Cat2BugStatistic.refreshData` 注册 `TeamPlanCountdown`（`PersonalRemindTimer` 可选 no-op）
- [x] 4.4 确认 `StatisticTemplate.vue` 个人/团队分组可见新块

## 5. 前端：i18n

- [x] 5.1 7 语言：`defect.personal-remind-timer.*`、`defect.team-plan-countdown.*`（标题、指标、逾期、调度类型、音效名、空态）

## 6. 后端（按需）

- [x] 6.1 仅当 `getPlan` 权限不足时：在 `SysDefectStatisticController` 增加 `plan/{planId}/summary`，返回倒计时所需字段子集，`system:defect:query`

## 7. 验证

- [x] 7.1 按 `TESTING.md` 手工验收（实现完成，待本地运行确认）
- [x] 7.2 确认默认 7 块与用户已有模板未被自动修改
