## Context

甘特排期、`levelDefectPlans`、`pushOverlapsAfterPinned`、FS「次日」目前全部按自然日（`addDays` / `planDurationDays`）。轴仅 `weekend: Sat|Sun` 底纹。自动排列相关变更曾明确排除工作日历。产品要 OmniPlan 式可编辑项目日历，并把排期语义切到工作日（含中国调休）。

约束：Quarkus + Vue3；可私有化离线；写缺陷仍走既有 `updateDefect`；日历编辑权限对齐项目设置（创建人/管理员）；不推翻已有 ADR。

## Goals / Non-Goals

**Goals:**

- 每项目一份可编辑工作日历：工作周 + 年例外（`holiday` / `workday` 调休）。
- 内置中国法定底表按年导入到项目副本；可手工改；缺年仅工作周 + 提示。
- 甘特：工期/落点/自动排列/拖动避让/FS 次日一律走工作日；轴假日/调休可视；负荷轨非工作日不计占用。
- 一键自动排列可选「本次忽略日历」。
- 不自动改写存量 plan 起止。

**Non-Goals:**

- 个人班次、半天、多时区、团队级日历实体（可后补复制模板）。
- 外网实时假日 API、服务端资源求解器。
- 批量「按工作日重算存量结束日」工具（可后续）。
- SS/FF/SF、改处理人/交付物作为平整手段。

## Decisions

1. **项目级单日历 + JSON 例外**  
   新建表（建议 `sys_project_work_calendar`）：`project_id` PK、`work_week`（如 `[1,2,3,4,5]`，周一=1）、`exceptions` JSON 映射 `YYYY-MM-DD → holiday|workday`、审计字段。无行时服务端/前端使用**默认日历**（一～五、无例外）等价行为。  
   **备选**：塞进 `sys_project` 大 JSON——否决，独立表更清晰、权限与 MCP 卡同模式。  
   **备选**：团队级共享实体——否决为首版；日后「从团队模板复制」即可。

2. **假日数据：内置底包 + 导入到项目副本**  
   产品资源（classpath 或 `resources/holidays/cn-YYYY.json`）提供近年国务院安排（假日+调休）。编辑器「导入中国法定」将该年写入项目 `exceptions`（可配置：覆盖该年键 / 仅填充空缺）。不调用外网。新一年靠发版补包或用户手工点选。  
   **备选**：启动拉第三方 API——否决（私有化/SLA）。  
   **备选**：仅全局只读不可改——否决（产品要求 OmniPlan 式可编）。

3. **工作日判定唯一入口**  
   前端 `defect-gantt.js`（及必要时后端工具类，若日后服务端也排）统一：  
   `isWorkday(d, cal)` → 例外 `workday` 优先 true；`holiday` false；否则 `weekday ∈ workWeek`。  
   `addWorkdays` / `nextWorkday` / `countWorkdays` / `endExclusiveFromWorkdays`。  
   排期路径注入项目日历；「忽略日历」时传入空例外 + 全周工作（或显式 `null` 退回自然日，见 Open Questions 拍板为「全自然日」）。

4. **存库仍是自然起止；显示与算法用工作日**  
   `plan_start` / `plan_end` 语义不变（日粒度时间戳）。条宽仍按自然跨度画。`planDurationDays` 改名为工作日计数（或新函数 `planDurationWorkdays`，旧名委托并注明）。拖边最小 1 **工作日**。未排期落入非工作日时对齐到 `nextWorkday`（或提示后对齐）。

5. **FS「次日」= 前置结束日之后的下一工作日**  
   `fsEarliestStartAfterEnd` 改为 `nextWorkday(end)`（若 end 当日仍是工作日且产品「次日」不含当天，则从 end+1 自然日再找下一工作日——与现「结束次日」对齐：`nextWorkday(addDays(end,1))` 若 end 当天结束占用整天）。实现时与现「结束日当日不算后继可开工」保持一致，仅跳非工作日。

6. **UI 入口**  
   - 编辑器为共享 `el-dialog`（非独立路由页）；项目设置「基本信息」卡与甘特图层菜单均可 `open()`。  
   - 创建人/管理员可编；成员只读可看规则。  
   - 内容：工作周勾选 + 年月历点选切换状态 + 例外列表 + 按年导入；保存/导入后向父组件 `saved`，甘特侧刷新日历快照。

7. **负荷轨**  
   占用只统计工作日列；非工作日格子不贡献 overload，样式可灰。

8. **权限**  
   读：项目成员。写：`project.create-by` / `project.admin`（与项目设置其它卡一致）。排期写缺陷仍要 `system:defect:edit`。

## Risks / Trade-offs

- [存量条「天数」显示变了但起止未变] → 文案标明工作日；不做静默重算。  
- [国庆等长假 ASAP 大幅右移] → 预期行为；预览摘要可提示跨越假日。  
- [内置表过期/缺年] → 降级 + 提示；支持手工补。  
- [前后端日历不一致] → 首版排期算法在前端；日历 CRUD 在后端；甘特加载时拉日历快照。  
- [导入覆盖用户改的公司假] → 导入 UI 明确「覆盖该年 / 仅填空」；默认仅填空或二次确认。  
- [「忽略日历」语义歧义] → 规格写死为本次自动排列按自然日；不持久改日历。

## Migration Plan

1. Flyway 建表；老项目无行 = 默认工作周。  
2. 随发版带上近年 `cn-YYYY.json`；新建项目可选自动导入当前年（建议：**不**自动导入，避免无提示写入；首次打开编辑器引导导入）。  
3. 前端甘特依赖日历 API；失败时降级默认工作周并 toast。  
4. 回滚：藏编辑入口、甘特退回自然日开关或忽略日历常开——以 feature 开关或回退提交为准。  
5. 回写 `docs/04`、`docs/05`、`docs/08`；若确认「项目日历为排期真相源」则补 ADR。

## Open Questions

- 新建项目是否默认导入当前年中国法定？（设计倾向：否，首次引导。）  
- 「忽略日历」仅一键自动排列，还是拖动也可临时忽略？（倾向：仅一键预览勾选。）

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`
- `docs/05-接口文档/接口清单-项目团队成员.md`
- `docs/02-系统架构/数据库.md`
- `docs/08-版本记录/2026-10.md`
- `docs/06-踩坑记录/Element-Plus表单项内容区横排挤开子块.md`
- 未新增 ADR（日历为排期真相源已落在 OpenSpec `project-work-calendar` 与实现；若后续要跨功能硬约束再补）
