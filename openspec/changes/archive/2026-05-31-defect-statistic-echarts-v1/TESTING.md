# 缺陷统计 ECharts v1 — 手工测试清单

验证 `defect-statistic-echarts-v1`：个人「我的待办」环形图 + 团队「待办负载」条形图 + 未关闭待办 API。

## 前置条件

- JDK 17+，后端 `cd cat2bug-platform-admin && mvn spring-boot:run`
- 前端 `cd cat2bug-platform-ui && npm run dev`，访问 `http://localhost:2222`
- 登录有缺陷查询权限的账号，进入某项目 **缺陷** 页（表格视图）
- 项目中准备数据：
  - 至少 2 名成员各自有未关闭缺陷
  - 至少 1 条缺陷 `handle_by` 含 2 人（验证重复计数）
  - 至少 1 条 `CLOSED` 缺陷（验证不计入）

## 1. API

- [ ] `GET /prod-api/system/defect/statistic/open-workload/{projectId}` 返回 200，数组 ≤5，含 `total`、`processing`、`audit`、`rejected`
- [ ] `GET /prod-api/system/defect/statistic/open-workload/{projectId}/my` 返回当前用户汇总
- [ ] 多人共管缺陷：两名成员 `total` 各 +1（团队各成员 total 之和可大于项目未关闭总数）
- [ ] 已关闭缺陷不计入任何成员

## 2. 新用户默认模板

- [ ] 清除当前用户在该项目的统计模板（或换新测试账号/项目）
- [ ] 打开缺陷页，统计条展示 7 块：成员在线、交付物排行、状态统计、类型统计、**我的待办**、**团队待办负载**、我的人生
- [ ] 已有自定义模板的账号：打开后仍为原模板，未被覆盖

## 3. MyOpenTodoGauge（115px）

- [ ] 环图中心数字与 `/my` API 的 `total` 一致
- [ ] 统计块高度约 115px，与相邻 DefectState 对齐
- [ ] 无待办时显示空状态或 0，无控制台 ECharts 报错
- [ ] 点击环图/中心：列表筛选为当前用户 + 未关闭状态

## 4. TeamOpenWorkloadBar（115px）

- [ ] 条形图展示 Top5，顺序与 API 一致
- [ ] Y 轴昵称过长时截断，不撑破 115px 高度
- [ ] 点击某成员条：列表 `handleBy` 含该成员且未关闭

## 5. StatisticTemplate

- [ ] 进入统计模板页，可选列表含 `MyOpenTodoGauge`、`TeamOpenWorkloadBar`
- [ ] 点击添加后保存成功；重复添加提示已存在

## 6. 明暗主题

- [ ] 浅色主题下图表标签与扇区/柱条清晰
- [ ] 切换暗色主题后两 ECharts 块仍可读，无大面积白底

## 7. 回归

- [ ] 现有 DefectState、DefectType 点击筛选仍正常
- [ ] 统计条横向滚动与左右箭头仍可用（7 块默认时）
