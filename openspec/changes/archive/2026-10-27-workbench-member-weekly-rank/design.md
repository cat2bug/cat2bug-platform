## Context

工作台已有 `member-defect-rank`（`MemberOfDefectRank` + `GET /system/dashboard/{projectId}/member-defect`）：头像 + 两条进度条，数据来自缺陷日志累计/当日，**无周界、无 Git/文档**。周报与 `weekOps` 已统一「周一 00:00 ～ 下周一 00:00（左闭右开）」周界（`SysDashboardService.currentIsoWeekRange()`）。Git 贡献者 API 按仓聚合、偏全量，不直接满足「项目 × 本周 × 成员」。文档表 `sys_document` 有 `create_by_id` / `update_by_id` + 时间字段，可作「文件处理」轻量口径。

## Goals / Non-Goals

**Goals:**

- 工作台可添加 **成员本周贡献** 组件：头像、姓名、三维进度条；**无排名**，按展示名排序。
- 服务端一次返回本周成员贡献数据；前后端口径与 `weekOps` 周界一致。
- 目录分组 `member`，不破坏既有 `member-defect-rank`。

**Non-Goals:**

- 不做历史周切换、加权排序、归一化总分、未匹配 Git 作者专区。
- 不把「文件处理」做成 Git diff 文件数或文档隐藏仓提交数（可后续扩展）。
- 不替换缺陷专项排行；不新开 ADR（方案属既有工作台扩展）。

## Decisions

### D1. 指标口径

| 维 | 口径 | 备选否决 |
|--|--|--|
| 缺陷处理数 | 本周 `sys_defect_log` 中该用户作为 `create_by`、且缺陷属本项目的 **distinct `defect_id`**（与现排行同源，加周过滤） | 「仅 CLOSED」过窄，与「处理」日常语感不符 |
| 代码提交数 | 项目下各代码仓在默认/主 ref 上、本周 commit author 匹配到项目成员后的提交次数之和；匹配规则与浏览页一致（email 优先，否则 name） | 前端拉各仓 contributors 再拼：重复请求、无时间窗 |
| 文件处理数 | 本周 `sys_document`：`create_by_id` 或 `update_by_id` = 用户，且 `create_time`/`update_time` 落入周窗的文档数（文件+文件夹均可计；同一文档本周多次更新仍计 1） | Git 改文件数与「提交数」高度相关；文档 Git 历史更重 |

### D2. 排序与进度条

- **排序键** = `defectCount + commitCount + fileCount`（原始加总，降序）；同分可按 `userId` 稳定排序。
- **进度条百分比** = 各维 `count / max(本周该维所有成员)`；分母为 0 则 0%。三条使用不同 `el-progress` status/色（如默认 / success / warning），与缺陷排行视觉语言一致。
- 仅返回 `total > 0` 的成员；全员为 0 时返回空列表，前端展示空态文案。

### D3. API

- `GET /system/dashboard/{projectId}/member-weekly-rank`
- 权限：`system:dashboard:query`
- 响应 `data`: `{ weekStart, weekEnd, members: [{ userId, nickName, userName, avatar, defectCount, commitCount, fileCount, total }] }`，已按 `total` 降序。
- 实现落在 `SysDashboardService`：复用 `currentIsoWeekRange()`；缺陷/文档用 Mapper SQL；Git 在服务内遍历项目仓库、带时间截断统计后 merge。

### D4. 前端组件

- 新 widget id：`member-weekly-rank`，`group: 'member'`，`titleKey: 'workbench.widget.member-weekly-rank'`。
- 独立组件（推荐 `workbench/widgets/MemberWeeklyRank.vue`），可用 `BoardChartHost` 挂载或直接 `component` 指向该 Vue；布局对齐 `MemberOfDefectRank`（左头像右信息），姓名在进度条上方。
- 展示周范围标签（复用 `workbench.weekly.range` 或专用键）。
- 点击行：可选跳转成员缺陷筛（与缺陷排行类似）；一期可不做，避免三指标无统一落地页。
- 注册表 + i18n（zh/en/tw 等既有语言文件）+ 目录 live 预览；presets 可选加入 manager 行。

### D5. Git 性能护栏

- 每仓只遍历本周时间窗内的提交（JGit `RevWalk` + `commitTime` 过滤），达到仓内上限（建议 5000 commits/仓）即停并记 warning 日志。
- 无仓 / 用户无 pull 权限：该维全 0，不导致整接口失败。
- 一期不做缓存；若线上偏慢再单开变更加短 TTL 缓存。

## Risks / Trade-offs

| 风险 | 缓解 |
|--|--|
| 原始加总被提交数主导 | 接受一期产品选择；文案标明「三项之和」；后续可归一化 |
| Git 扫仓慢 | D5 时间截断 + 上限；失败降级为 0 |
| Author 匹配不上 → 提交不进榜 | 与贡献者页同一规则；文档说明「需邮箱/名与成员一致」 |
| 「文件处理」被理解成附件/用例文件 | i18n 用「文档处理」更贴切；proposal 已锁定文档口径 |
| 与 `member-defect-rank` 并存混淆 | 标题强调「本周」「综合」 |

## Migration Plan

- 纯新增接口与组件；无 DB migration。
- 回滚：下线 widget 注册与路由即可；无数据迁移。

## Open Questions

- （无阻塞）是否默认加入 manager / developer 预设 —— 实现时看预设行是否已满，可仅进目录。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/01-产品需求/产品定位与核心功能.md`（工作台组件能力，实现后补一行）
- `docs/02-系统架构/数据库.md`（`sys_document` / 缺陷日志事实）
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`（实现后补 dashboard 新路径）
- `docs/07-技术决策/ADR-021-工作台固定页与自定义私有共享页签.md`（工作台壳约束，不修改）
- 拟新增 ADR：无
