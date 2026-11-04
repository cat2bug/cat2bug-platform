## Context

缺陷表格与详情展示「存活时间」「驳回」，依赖 API 中的 `handleStartTime`、`handlePassTime`、`rejectCount`。历史 Spring 列表 SQL 曾 JOIN `sys_defect_log` 派生这三项；Quarkus 迁移后 `SysDefectMapper` 只查 `sys_defect` 基表，`rejectCount` 仅留在 Java 实体、无库列、驳回动作不递增。前端 `lifeTime()` 在两端字段缺失时都回退为 `new Date()`，列表表现为「存活时间:0分钟」「驳回:0」。

产品已确认：重开开启新周期；通过与关闭均冻结存活终点，取本周期内最晚一次结案；看板/Excel/甘特不展示这两项。

## Goals / Non-Goals

**Goals:**

- 列表与详情 API 稳定返回正确的周期派生字段，使存活时间非 0（有真实时长的缺陷）、驳回次数与本周期驳回操作一致
- 语义与日志类型对齐：`CREATE`/`OPEN` 为锚点；`PASS`/`CLOSED` 冻结；`REJECTED` 计数
- 前端格式化健壮，不再因缺字段恒显示 0 分钟

**Non-Goals:**

- 不为指标新增 DB 列或写时维护计数器
- 不改变缺陷状态机与流转 API
- 不在看板 / Excel / 甘特增加存活时间或驳回展示
- 不回填或改写历史日志

## Decisions

### D1. 读时从 `sys_defect_log` 派生，不落库

- **选择**：列表/详情在 Service 层（或 Mapper 子查询）按缺陷 ID 批量派生三字段。
- **理由**：历史即读时派生；周期语义依赖日志；加列需迁移与写路径同步，收益低。
- **备选**：写时维护 `reject_count` / `handle_*` → 否决（回填成本高，OPEN 重置易漏）。

### D2. 周期与冻结规则（产品定稿）

```
锚点 start = 该缺陷日志中 create_time 最大的 CREATE 或 OPEN；
            若无此类日志 → 回退 defect.createTime
终点 end   = 锚点之后（create_time >= start）的 PASS 与 CLOSED 中 create_time 最大者；
            若无 → 请求时刻（进行中时钟）
rejectCount = 锚点之后 defect_log_type = REJECTED(4) 的条数
```

- `handleStartTime` = start  
- `handlePassTime` = end（命名沿用前端契约；语义含「最晚 PASS/CLOSED」或「当前时刻」）  
- 存活展示 = `end - start`（前端格式化）

### D3. Enrich 放在 Service，批量查日志

- **选择**：在 `selectDefectPage` / 详情返回前，按本页缺陷 ID 一次查出相关日志（或专用聚合查询），再填入 `SysDefect`；可与现有 `DefectMemberEnricher` 并列新增 `DefectCycleMetricsEnricher`。
- **理由**：Mapper 纯列查询保持简单；H2/MySQL 双方言复杂子查询易碎；批量避免 N+1。
- **备选**：完全恢复旧列表 SQL JOIN → 可作为后续优化，首版优先 Java enrich 便于单测规则。

### D4. 前端仅消费字段 + 弱降级

- `lifeTime()`：优先 `handleStartTime`/`handlePassTime`；起点缺则用 `createTime`；终点缺则用 `now`；禁止「两端同时缺省成 now」。
- 展示面不改：`table.vue`、详情 `DEFECT_DETAIL_EXTRA_ROWS` 已用这两项。

### D5. 不新增 ADR

指标恢复属迁移回归修复，不改变架构选型；归档时在术语表/接口清单写清字段语义即可。

## Risks / Trade-offs

- [列表页日志批量查询成本] → 仅对本页 ID 聚合；必要时单条 SQL `GROUP BY defect_id`；监控列表耗时  
- [无 CREATE/OPEN 的脏数据] → 回退 `createTime`，避免再出现 0 分钟  
- [枚举 `REJECT` vs `REJECTED`] → 计数严格用 `REJECTED`(4)；单测锁定  
- [字段名 `handlePassTime` 含 CLOSED] → 文档与注释标明「结案冻结时刻」，避免后续误删 CLOSED 语义  

## Migration Plan

- 无 Flyway；部署后新旧前端均可读新字段；旧前端若仍双 `now` 回退，需同步前端修复  
- 回滚：去掉 enrich 即可，无 schema 回滚  

## Open Questions

- 无（产品规则已定）

## 相关长期文档

- `docs/01-产品需求/术语表.md`（缺陷状态、缺陷日志）
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`
- `docs/02-系统架构/数据库.md`（`sys_defect_log`）
- 拟归档回写：术语表增「存活时间 / 周期驳回次数」一行；接口清单注明列表/详情派生字段  
- 拟新增 ADR：无
