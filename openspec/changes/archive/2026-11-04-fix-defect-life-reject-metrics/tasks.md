## 1. 后端周期指标派生

- [x] 1.1 确认 `SysDefect` 已有 `handleStartTime`/`handlePassTime`/`rejectCount` 可序列化；若缺则补字段与 getter/setter（不落库）
- [x] 1.2 实现 `DefectCycleMetricsEnricher`（或等价）：按缺陷 ID 批量读取日志，计算周期锚点、最晚 PASS/CLOSED 终点、REJECTED 计数并回填
- [x] 1.3 在 `SysDefectService` 列表分页与详情路径于成员 enrich 之后调用指标 enrich；无日志时起点回退 `createTime`，无结案时终点为当前时刻
- [x] 1.4 补充后端单测：进行中非 0、PASS/CLOSED 冻结取最晚、OPEN 重置存活与驳回、只计 REJECTED(4)

## 2. 前端存活时间格式化

- [x] 2.1 修改 `cat2bug-platform-ui/src/utils/defect.js` 的 `lifeTime()`：优先 API 起止；起点缺用 `createTime`；禁止双端同时 `new Date()`
- [x] 2.2 为 `lifeTime` 增加前端单测（缺字段降级、有 handlePassTime 冻结、非 0 分钟）
- [x] 2.3 确认表格与详情仍读 `rejectCount`/`lifeTime`；看板/Excel/甘特不新增展示

## 3. 验证

- [x] 3.1 本地打开缺陷表格：创建超过一分钟的未结案缺陷存活时间非「0分钟」；驳回后计数增加；重开后存活与驳回重置；通过/关闭后刷新存活不再增长

## 4. 回写长期知识

- [x] 4.1 在 `docs/01-产品需求/术语表.md` 增加「存活时间 / 周期驳回次数」一行（周期锚点、冻结规则）
- [x] 4.2 在 `docs/05-接口文档/接口清单-缺陷与用例计划.md` 注明列表/详情派生字段 `handleStartTime`/`handlePassTime`/`rejectCount`
- [x] 4.3 无新 ADR；若实现中踩坑再补 `docs/06-踩坑记录/`
