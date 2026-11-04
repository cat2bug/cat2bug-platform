## Why

缺陷列表与详情里的「存活时间」「驳回」几乎恒为 0：Quarkus 迁移后列表/详情不再从 `sys_defect_log` 派生 `handleStartTime` / `handlePassTime` / `rejectCount`，前端 `lifeTime()` 在字段缺失时两端都回退为当前时刻，相减即为 0。指标是缺陷处理效率的基本可见性，需按周期语义恢复。

## What Changes

- 后端在缺陷列表与详情响应中恢复/填充周期派生字段：`handleStartTime`、`handlePassTime`（本周期结案冻结时刻）、`rejectCount`
- 周期锚点为最近一次日志 `CREATE` 或 `OPEN`；重开后存活与驳回均从新周期重新计算
- 本周期内尚无 `PASS`/`CLOSED` 时，存活终点为「当前时刻」；一旦有通过或关闭，终点取本周期内**最晚一次** `PASS` 或 `CLOSED` 并冻结
- 驳回次数统计本周期内 `REJECTED`（日志类型 ordinal=4）次数；不计入 `REJECT` 等其它类型
- 前端 `lifeTime()` 消费后端派生字段；缺字段时不再双端回退成 `now` 导致恒 0（合理降级：起点可用 `createTime`）
- **不**在看板、Excel、甘特视图新增这两项展示（仍仅表格标题下统计行与详情固定统计行）

## Capabilities

### New Capabilities

- `defect-life-reject-metrics`：缺陷存活时间与周期驳回次数的计算规则、API 字段约定与既有展示面行为

### Modified Capabilities

- （无）既有 openspec/specs 未定义这两项指标的需求契约

## Impact

- 后端：`SysDefect` 派生字段约定、列表/详情查询或 enrich（`SysDefectMapper` / `SysDefectService` / `DefectResource`）；依赖 `sys_defect_log`
- 前端：`cat2bug-platform-ui/src/utils/defect.js` 的 `lifeTime()`；表格与详情已有展示无需扩面
- 数据库：不新增表列（按日志实时派生；不写时维护 `reject_count`）
- 文档：归档前在术语表或接口清单补指标定义（长期事实）
- 不推翻现有 ADR
