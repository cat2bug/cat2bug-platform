# 手工测试：缺陷统计条 — 团队走势图（状态 + 成员）

## 前置

- 后端 `mvn spring-boot:run -pl cat2bug-platform-admin`
- 前端 `cd cat2bug-platform-ui && npm run dev`
- 账号仅有 `system:defect:query`（**无** `system:dashboard:query`）亦可验证新块
- 项目内有多日缺陷 `update_time` 变更、多名成员有 `sys_defect_log` 记录

## 模版自选

1. 默认统计模板打开缺陷页 → **不含** 两个走势块
2. 统计模版 → 团队区添加「缺陷状态走势」「成员处理走势」→ 保存后缺陷页出现两块
3. 两块宽度与「测试计划燃尽」相近（约 320px），总高度 ≤ 115px

## TeamDefectStateTrend

1. 默认 30 天折线加载；切换 12 个月 → x 轴与数据更新
2. 系列包含各缺陷状态（与仪表盘同项目对比口径一致，可用有 dashboard 权限账号对照）
3. 点击某日「处理中」数据点 → 列表仅该状态且 `update_time` 在该日范围内
4. 月模式点击某月某状态 → 列表 `update_time` 落在该月内
5. 导出 Excel → 文件可打开且与当前 `timeType` 一致
6. 统计模版预览（read）→ 点击不筛选列表；导出不触发
7. 暗色主题下图表与坐标轴可读
8. `refreshData` / 缺陷页刷新 → 图表数据更新

## TeamMemberDefectTrend

1. 折线数为项目内有处理记录的**全员**（非 Top5）
2. 图例可滚动辨认各成员
3. 日模式：点击成员 A 某日点 → 列表为 A 在该日有参与日志的缺陷（与热力图联动语义一致）
4. 月模式：点击成员 A 某月点 → 列表为 A 在该月有参与日志的缺陷
5. 导出、read 不联动、主题、刷新 — 同状态走势块
6. 仅 defect 权限账号可加载数据（不依赖 dashboard API）

## 回归

1. `DefectState` 快照块、`TeamPlanBurndown`、`MyParticipationHeatmap` 行为不变
2. 仪表盘两张原图仍可正常使用
3. 默认 7 块未被自动插入新块
