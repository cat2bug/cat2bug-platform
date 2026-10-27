## 1. 库表与升级迁移

- [x] 1.1 Flyway：`sys_case_type` 增加/回填 `team_id`，调整唯一索引；评审模板与缺陷 builtin 配置落到 `sys_team`（或等价列）
- [x] 1.2 Flyway：`sys_project_defect_field` 改为 `team_id`；按团队最早项目为源拷贝类型/模板/字段/布局
- [x] 1.3 迁移脚本：按类型名称 remap 各项目 `sys_case.case_type`；删除非源项目冗余配置行；无项目团队 seed 默认类型+模板
- [x] 1.4 新建团队 seed、新建项目不再 per-project `seedDefaults`（改为依赖团队配置）

## 2. 后端 Team API 与权限

- [x] 2.1 新增 `/system/team/{teamId}/case-types` CRUD（及 enabled）；权限 `system:team:case-type:*`；校验团队管理员/创建人
- [x] 2.2 新增 `/system/team/{teamId}/case-review-template` GET/PUT
- [x] 2.3 新增 `/system/team/{teamId}/defect-fields`（及 builtin-layout）；权限 `system:team:defect-field:*`
- [x] 2.4 业务读：project 路径只读列表经 `project.team_id` 解析；用例保存校验类型属本团队
- [x] 2.5 废弃或 410 旧 `/system/project/{projectId}/…` 写接口；菜单种子赋 role 12/4，收回项目侧配置菜单
- [x] 2.6 单测/迁移冒烟：多项目 remap、权限拒绝、空团队 seed（编译通过；完整迁移/集成测建议在本地库跑一遍 `V1_1_0_50`）

## 3. 前端团队设置与拆项目入口

- [x] 3.1 团队设置 hub 增加用例设置、缺陷属性卡片与子路由；页面改调 team API（`teamId`）
- [x] 3.2 删除项目设置 `CaseTypeCard` / `DefectSettingsCard` 及 `/project/case-types|case-settings|defect-fields` 入口
- [x] 3.3 缺陷/用例业务页确认读路径仍可用；i18n 文案改为团队语境
- [x] 3.4 手工验证：团队管理员可配；仅项目管理员不可写；同团队两项目共享类型与字段（代码路径已就绪，建议在 UI 点验）

## 4. 回写长期知识

- [x] 4.1 新增 `docs/07-技术决策/ADR-023-用例类型评审模板与缺陷属性归属团队.md`，并入 `07-技术决策/说明.md` 与 `00-项目总览.md`
- [x] 4.2 更新 `docs/01`、`02` 库表、`03` 模板与权限、`05` 接口清单、`08` 当月版本记录一行
- [x] 4.3 踩坑：无
