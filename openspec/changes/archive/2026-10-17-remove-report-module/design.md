## Context

报告是项目侧栏里的一个模块：正文存在 `sys_report`，模版存在 `sys_report_template`，编辑历史存在 `sys_report_history`，批注复用评论表且 `module_type` 为 `report` / `report_history`。开放接口 `POST /api/report/defect` 先写报告，再按 payload 创建或更新缺陷。计划表有可空列 `sys_plan.report_id`，没有外键。

产品决定不再提供报告，已有报告数据不导出。这推翻 `docs/07-技术决策/ADR-009-报告模版编辑器用Milkdown且DSL为真相源.md`。

Flyway 同时维护 `db/migration`（H2）与 `db/mysql`。已发布脚本的校验和不能改。

## Goals / Non-Goals

**Goals:**

- 运行中的系统不再提供报告页面、接口、菜单、权限、MCP 工具和新建报告通知。
- 升级后三张报告表、报告评论和 `sys_plan.report_id` 被删除，缺陷与计划行保留。
- 全新安装也不再创建这些表和菜单（靠后续迁移删掉初始化脚本里的定义，不改 `V0_5_1`）。

**Non-Goals:**

- 不改缺陷、用例、计划项、文档的业务行为。
- 不保留或导出已有报告。
- 不改登录页「分析报告」宣传句，不改 MCP prompt `report_defect`（提交缺陷）。
- 不改 `test-standards-skills`：它描述标准里的测试文档种类，不是这个产品模块。

## Decisions

### 1. 数据用新迁移删除，不改历史脚本

新增 `V1_1_0_43__drop_report_module.sql`，H2 与 MySQL 各一份。脚本删除：

- `sys_plan.report_id`
- 评论中 `module_type` 为 `report`、`report_history` 的行
- `sys_report_history`、`sys_report`、`sys_report_template`
- `perms` 以 `system:report:` 开头的菜单、组件为报告页的菜单，以及 `sys_role_menu` 中的对应行

`V0_5_1__init.sql` 仍会建表和菜单，随后由 `V1_1_0_43` 删掉。这样已升级的库与全新安装走同一条删除路径。

备选：直接改 `V0_5_1`。否决，因为已执行库的 Flyway 校验和会对不上。

### 2. 开放接口整段删除，不把缺陷写入挪到别处

删除 `OpenApiReportResource` 与 `OpenApiReportService`。`POST /api/report/defect` 返回不存在。JUnit / JLog 若只靠这个地址推缺陷，升级后推送失败；缺陷本身仍可由既有缺陷接口写入。

备选：保留该地址、只写缺陷、不再写报告。否决，本次选择整段删除。

### 3. 评论表保留，只删报告行

`sys_comment` 继续给缺陷使用。报告批注没有正文可挂，随迁移删除。

### 4. Git 浏览用的标题提取先挪再删

`views/system/git/repo/browse.vue` 使用 `components/Report/annotation/report-toc.js` 的标题提取。把这份与报告无关的函数挪到 Git 或通用 Markdown 工具下，再删除 `components/Report/` 与 `components/ReportTemplateEditor/`。`@milkdown/*` 从 `package.json` 移除。

### 5. 横切引用按「不再出现报告模块」清理

一并去掉：仪表盘 `/{projectId}/report`、项目云备份里的报告 JSON、classpath `报告/模版/`、预制模版同步脚本、个人通知里的报告事件、MCP `list_reports` / `get_report` 与 `get_guide` 的 `report` 主题、项目模块白名单与旧路径 `/project/report`、快捷键里的报告项、各语言包的 `report.*`。项目 MCP 已保存的开放模块列表若含 `report`，迁移时去掉该值，设置页不再显示报告。

Native 资源包含列表去掉 `报告/**`。

### 6. 新 ADR 取代 ADR-009

新增 `docs/07-技术决策/ADR-018-移除报告模块且不保留历史数据.md`，并在 ADR-009 与 `docs/07-技术决策/说明.md` 标明被 ADR-018 取代。归档前按知识库约定回写 `docs/01`～`05` 与当月版本记录。

## Risks / Trade-offs

- [已有报告正文、批注、模版永久丢失] → 已确认不导出。上线前不再提供恢复窗口。
- [JUnit / JLog 的 `/api/report/defect` 失效] → 在版本说明里写明。缺陷接口仍在。
- [代码回滚后表已不在] → 不准备向下迁移。回滚代码必须同时恢复删除前的库备份。
- [误删 Git 标题提取] → 任务里把挪文件放在删目录之前，并用现有浏览页测试守住。
- [初始化脚本与删除脚本之间的窗口] → 只在同一次 Flyway 运行内发生，应用启动完成前用户进不了报告页。

## Migration Plan

1. 发布带 `V1_1_0_43` 的版本。启动时 Flyway 删除表、列、评论行和菜单。
2. 前端不再注册报告路由；旧书签 `/…/report` 不再解析到报告页。
3. 不做数据回填。需要旧报告的环境应在升级前自行备份整库，本变更不提供导出。

## Open Questions

无。

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/01-产品需求/产品定位与核心功能.md`
- `docs/03-技术方案/报告模版DSL与Milkdown编辑器.md`
- `docs/03-技术方案/MCP接入与权限.md`
- `docs/05-接口文档/接口清单-报告与文档.md`
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`
- `docs/07-技术决策/ADR-009-报告模版编辑器用Milkdown且DSL为真相源.md`
- 拟新增：`docs/07-技术决策/ADR-018-移除报告模块且不保留历史数据.md`
