## 1. 数据库与权限

- [x] 1.1 新增 `db/migration` 与 `db/mysql` 的 `V1_1_0_43__drop_report_module.sql`：删除 `sys_plan.report_id`、报告评论行、三张报告表、`system:report:*` 菜单及角色关联
- [x] 1.2 迁移中去掉项目 MCP 开放模块配置里的 `report`
- [x] 1.3 不修改已发布的 `V0_5_1__init.sql`

## 2. 后端

- [x] 2.1 删除报告、报告模版、报告历史、预制模版、OpenAPI `POST /api/report/defect` 的资源、服务、Mapper 与领域对象
- [x] 2.2 去掉仪表盘报告统计、Excel 报告导出、项目云备份中的报告 JSON、新建报告的 IM 通知与正文模版
- [x] 2.3 去掉 MCP `list_reports`、`get_report`、通用通道里的报告分支，以及 `get_guide` 的 `report` 主题与 `mcp-guide` 报告正文
- [x] 2.4 评论删除规则去掉仅针对报告的分支；计划查询去掉 `reportId`
- [x] 2.5 删除 classpath `报告/模版/`、预制模版同步脚本，并从 Native 资源包含列表去掉 `报告/**`
- [x] 2.6 删除只覆盖报告的测试，并改掉仍引用报告接口的测试与冒烟

## 3. 前端

- [x] 3.1 把 Git 浏览使用的 Markdown 标题提取挪出 `components/Report/`，确认浏览页仍能出目录
- [x] 3.2 删除报告页面、报告组件、模版编辑器、模版选择器、报告 API，以及项目模块白名单、路由、旧路径和快捷键中的 `report`
- [x] 3.3 去掉仪表盘报告统计、通知设置里的报告事件、各语言包 `report.*`，并从 `package.json` 移除 `@milkdown/*`
- [x] 3.4 更新仍访问 `/…/report` 的前端测试

## 4. 技能与设计资料

- [x] 4.1 删除仅服务于报告模版的 `readme/development/skills/cat2bug-report-dsl/`、`readme/development/skills/report-templates/` 与 `readme/design-system/report-template-dsl-mapping.md`

## 5. 回写长期知识

- [x] 5.1 新增 `docs/07-技术决策/ADR-018-移除报告模块且不保留历史数据.md`，并把 ADR-009 标为被 ADR-018 取代
- [x] 5.2 回写 `docs/00-项目总览.md`、`docs/01-产品需求/产品定位与核心功能.md`、`docs/03-技术方案/MCP接入与权限.md`、`docs/05-接口文档/接口清单-报告与文档.md`、`docs/05-接口文档/接口清单-缺陷与用例计划.md`，以及 `docs/08-版本记录` 当月一行；报告模版 DSL 笔记改为指向 ADR-018 的退役说明
- [x] 5.3 踩坑：无
