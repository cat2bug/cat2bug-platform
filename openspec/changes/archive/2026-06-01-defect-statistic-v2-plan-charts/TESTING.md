# 手工测试：缺陷统计条 v2（参与热力 + 计划图表）

## 前置

- 后端 `mvn spring-boot:run -pl cat2bug-platform-admin`
- 前端 `cd cat2bug-platform-ui && npm run dev`
- 使用具备 `system:defect:query` 的账号，**无** `system:dashboard:query` 的账号也应能加载新 API
- 项目内：至少 1 个测试计划、若干 `sys_plan_item` 执行记录；当前用户对若干缺陷有 `sys_defect_log`

## 模版自选（不进入默认条）

1. 新用户或清空当前项目统计模板后打开缺陷页 → 仅见 v1 默认 **7** 块，**不含** 参与热力/计划燃尽/雷达
2. 进入「统计模版」→ 个人区可见「我的参与」→ 添加后缺陷页出现热力条
3. 团队区添加「测试计划燃尽」「测试计划雷达」→ 缺陷页出现对应块

## MyParticipationHeatmap

1. 块高度 ≤ 115px，近 30 日单行热力可见
2. tooltip 显示日期与次数；无日志日为浅色/0
3. 点击有参与的日子 → 列表仅显示当日当前用户有日志的缺陷
4. StatisticTemplate 预览（read）点击不筛选列表
5. 暗色主题下图表可读

## TeamPlanBurndown

1. 下拉切换计划 → 图表更新
2. 与仪表盘同计划燃尽数据一致（抽样 1 个计划对比）
3. 无计划时空状态
4. 无 dashboard 权限用户可正常加载

## TeamPlanMetricsRadar

1. 6 轴雷达（发现/修复/探测/严重/重开/逃逸），数值 0–100
2. 计划 > 5 时仅绘 Top5，界面提示总计划数
3. tooltip 可辨认计划名

## 刷新

1. 新增缺陷日志或执行用例后，调用缺陷页刷新（或触发 `refreshData`）→ 三块数据更新

## 回归

1. v1 `MyOpenTodoGauge`、`TeamOpenWorkloadBar` 行为不变
2. 默认 7 块顺序与 v1 一致
