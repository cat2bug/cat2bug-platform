## 1. 后端聚合 API

- [x] 1.1 新增 DTO（如 `SysMemberWeeklyRank` / 响应包装：weekStart、weekEnd、members）
- [x] 1.2 Mapper：本周按成员聚合缺陷 distinct 数（`sys_defect_log` + 项目过滤 + `currentIsoWeekRange`）
- [x] 1.3 Mapper：本周按成员聚合文档处理数（`sys_document` create/update 落入周窗，按 doc 去重）
- [x] 1.4 Service：遍历项目代码仓，本周 commit + author→成员匹配累加提交数（时间截断、每仓上限、单仓失败降级）
- [x] 1.5 `SysDashboardService` 合并三维、算 total、过滤 0、按展示名排序（无排名）；暴露 `GET /system/dashboard/{projectId}/member-weekly-rank`（`system:dashboard:query`）
- [x] 1.6 补充必要单测或 Mapper/H2 冒烟（周界、排序、空列表）

## 2. 前端组件与注册

- [x] 2.1 `api/system/dashboard.js` 增加 `memberWeeklyRank(projectId)`
- [x] 2.2 实现 `MemberWeeklyRank.vue`（头像、姓名、三进度条不同色、周范围、空态、loading）
- [x] 2.3 `widget-registry` 注册 `member-weekly-rank`（`group: member`）；接入 `BoardChartHost` 或直接挂 component
- [x] 2.4 多语言 `workbench.widget.member-weekly-rank` 及指标/空态文案
- [x] 2.5 确认目录 live 预览与已用标记正常；可选加入 manager/developer 预设

## 3. 回归与回写

- [x] 3.1 手工验证：有缺陷/文档/提交数据的项目本周贡献列表与进度条比例；无仓时提交为 0 仍列出；全 0 空态；无名字/名次角标
- [x] 3.2 回写长期知识：`docs/01` 工作台组件一行、`docs/05` 接口清单一行、`docs/08` 当月版本记录；无新 ADR / 无踩坑则注明
