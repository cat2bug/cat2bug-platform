# 手工测试：缺陷统计条 — 个人报时 + 团队计划倒计时

## 前置

- 后端 `mvn spring-boot:run -pl cat2bug-platform-admin`
- 前端 `cd cat2bug-platform-ui && npm run dev`
- 账号具备 `system:defect:query`；至少 1 个测试计划含 `planEndTime`、用例与缺陷计数
- 浏览器允许站点播放声音（首次可在对话框点「试播」）

## 模版自选（不进入默认条）

1. 默认统计模板打开缺陷页 → 仍为 v1/v2 默认块，**不含** 报时与计划倒计时
2. 统计模版 → 个人区添加「报时提醒」→ 缺陷页出现 `PersonalRemindTimer`
3. 团队区添加「测试计划倒计时」→ 出现 `TeamPlanCountdown`

## PersonalRemindTimer

1. 块高度 ≤ 115px；无规则时空状态或引导点击配置
1a. 倒计时数字为 **Nixie One** 电子管风格（暖色辉光）；暗色主题下仍清晰
2. 点击卡片 → 对话框可新增多条规则（daily/weekly/monthly/once）
3. 保存模版后刷新 → 规则保留
4. 设 1–2 分钟内 `once` 规则 → 停留缺陷页 → 到点播放音效；离开缺陷页 → 到点不响
5. `once` 触发后不再重复；`daily` 次日同一时刻可再响（可选次日验证）
6. `monthly` day=31，在 2 月应在月末触发（可改系统日期或 mock 单元测试辅助）
7. 统计模版预览（read）→ 到点不播放
8. 页签切到后台 ≥1 分钟再回前台 → 倒计时应重算，不应在后台连播
9. 暗色主题下卡片与对话框可读

## TeamPlanCountdown

1. 下拉切换计划 → 天数与四指标更新
2. `planEndTime` 在未来 → 显示剩余 N 天，主数字为 Nixie 电子管字体，非红色逾期样式
3. `planEndTime` 在过去 → 「逾期 N 天」主数字为 Nixie + 红色辉光（`--overdue`）
4. 核对指标：`未完成=unexecutedCount`，`已完成=passCount`（与 fail 无关）
5. 核对缺陷：`未解决=defectCount-defectCloseStateCount`，`已解决=defectCloseStateCount`
6. 无计划时空状态；无 `planEndTime` 时友好提示
7. 触发 `refreshData`（或缺陷页刷新）→ 数据更新
8. 无 `system:plan:query` 仅有 defect 权限账号：若设计走 summary API，应能加载；否则记录为阻塞缺陷

## 回归

1. `MyLife`、`TeamPlanBurndown`、`MyParticipationHeatmap` 行为不变
2. 默认 7 块未被自动插入新块
