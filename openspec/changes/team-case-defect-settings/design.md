## Context

用例类型（`sys_case_type.project_id`）、评审模板（`sys_project.case_review_template`）、缺陷属性（`sys_project_defect_field` + `defect_builtin_field_config`）均按项目隔离；入口在项目设置卡片，写权限偏项目管理员（缺陷属性菜单已含团队角色）。团队设置目前仅基础信息与成员。产品要求三项升为团队唯一真相，入口进团队设置，仅 `team.create-by` / `team.admin` 可配。

约束：项目必属团队（`sys_project.team_id NOT NULL`）；允许仅项目成员非团队成员（`project-member-without-team`）——他们可继续**使用**配置，不可维护。鉴权角色并集依赖 `current_team_id` / `current_project_id`。

## Goals / Non-Goals

**Goals:**

- 团队级存储 + `/system/team/{teamId}/…` 写 API；业务读经 `projectId → teamId`。
- 团队设置 UI 覆盖类型 / 模板 / 缺陷属性；删除项目设置对应入口。
- 升级迁移：每团队以**最早创建的项目**为源；remap 用例 `case_type`；无项目团队 seed 默认类型+模板+空自定义字段。
- 权限菜单仅赋 role 12/4。

**Non-Goals:**

- 不做项目级覆盖层、不做「每项目再选一套」。
- 不改缺陷/用例业务流转本身（除类型归属校验与读配置路径）。
- 不把其它项目设置项（AI、MCP、Git 等）一并上收。
- 不引入拼音搜索或跨团队共享配置。

## Decisions

### D1. 数据模型：列改 `team_id`（原地升维）

- `sys_case_type.project_id` → `team_id`；唯一约束改为 `(team_id, name)`。
- 评审模板：新增 `sys_team.case_review_template`（或团队配置表一列）；迁移后可清空 `sys_project.case_review_template`（或保留只读废弃列一期只停写）。
- `sys_project_defect_field.project_id` → `team_id`；`sys_project.defect_builtin_field_config` → `sys_team` 对应列。
- `sys_defect.custom_fields` 仍按 `field_key`，键空间变为团队级。

备选：新建 `sys_team_*` 表再双写——否决，一期成本高；原地改列 + 迁移脚本足够。

### D2. 迁移：源项目 = 团队内 `create_time` 最早的项目

- 对每个 `team_id`：取 `ORDER BY create_time ASC, project_id ASC LIMIT 1` 为源。
- 拷贝源项目的类型行、模板、缺陷字段行与内置布局到团队维度（新 `type_id` 或保留 id 仅改 `team_id`——**优先保留源项目行 id、改归属列**，同团队其它项目类型行删除前先 remap）。
- Remap：非源项目用例 `case_type` 按**类型名称**匹配到团队（源）类型 id；名称无法匹配则置 `NULL`（可空类型已允许）。
- 非源项目的旧类型行、字段行删除；缺陷 `custom_fields` 中仅存在于非源且源无同 key 的键保留原值（只读展示容错），管理列表以团队定义为准。
- 无项目的团队：调用与现 `seedDefaults` 等价的团队 seed。

备选：首次打开团队设置弹窗选源——否决（用户已倾向自动；静默最早项目可重复、可测）。

### D3. API 边界

| 用途 | 路径 |
|--|--|
| 写/管理读 | `GET/POST/PUT/DELETE /system/team/{teamId}/case-types`…、`…/case-review-template`、`…/defect-fields`（及 builtin-layout） |
| 业务读 | 保留或薄封装 `GET /system/project/{projectId}/case-types`（只读启用列表）、`enabled`/`column-layout` 缺陷字段；内部查 team |
| 旧写接口 | **BREAKING** 删除或返回 410 |

权限：`system:team:case-type:list|add|edit|remove`、`system:team:defect-field:list|add|edit|remove`；模板编辑复用 `case-type:edit`。业务读继续 `system:case:list` / `system:defect:list` 等。

### D4. 前端

- `team/option/index.vue` 增加两张（或三张）卡片：用例设置（链类型+模板）、缺陷属性；子页复用现有 Vue，改 `teamId` API。
- 删除 `CaseTypeCard`、`DefectSettingsCard` 及 router 中 `/project/case-types|case-settings|defect-fields`（或仅重定向到团队设置并提示——提案要求**直接删除**入口）。
- 新建项目不再 per-project seed 类型；改为依赖团队已有配置（创建项目前团队应已 seed；建团队时 seed）。

### D5. 长期决策

拟新增 ADR：`ADR-023-用例类型评审模板与缺陷属性归属团队`（配置真相在团队、项目只消费）。

## Risks / Trade-offs

| 风险 | 缓解 |
|--|--|
| 非源项目同名不同类型语义被合并丢差异 | 文档说明以最早项目为准；名称 remap；无法匹配置空 |
| 仅项目成员无法进团队设置改配置 | 预期行为；需找团队管理员 |
| 迁移脚本失败半状态 | 单事务按团队；失败打日志可重跑幂等（以 team 已有类型则跳过拷贝） |
| 旧书签 `/project/case-types` 404 | 接受；可选短周期 redirect 到团队页（非目标） |

## Migration Plan

1. Flyway：加 `team_id` 列 / 团队模板与 builtin 列 → 回填 → remap → 删旧 project 独有行 → 收紧 NOT NULL / 唯一索引 → 废弃 project 写接口。
2. 发版前后端同时上：先库迁移，再开 team API、切前端、下线项目卡片。
3. 回滚：困难（数据已合并）；需备份；不承诺一键回项目级。

## Open Questions

- （无阻塞）废弃的 `sys_project.case_review_template` 列一期 DROP 还是留列停写——实现时选留列停写更安全。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/01-产品需求/产品定位与核心功能.md`
- `docs/02-系统架构/数据库.md`
- `docs/03-技术方案/用例评审纪要模板.md`
- `docs/03-技术方案/权限模型与鉴权链路.md`
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`
- `docs/08-版本记录/2026-10.md`
- 拟新增：`docs/07-技术决策/ADR-023-用例类型评审模板与缺陷属性归属团队.md`
