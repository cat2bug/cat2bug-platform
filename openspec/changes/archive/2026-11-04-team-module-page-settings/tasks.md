## 1. 数据模型与系统配置

- [x] 1.1 Flyway（H2 + MySQL）为 `sys_team` 增加 `module_page_config`，按 ADR-025 编号；NULL 读时视为全开默认
- [x] 1.2 新增/确认 `sys_config` 全局上传上限（MB），缺省 500，与现网 `FileUploadService` 对齐并可在系统后台修改
- [x] 1.3 后端模型与解析：`ModulePageSettings`（含 `case_plan` 双 label、`document.maxUploadMb`）及默认值 / 校验（文档上限 ≤ 系统上限）

## 2. 团队 / 项目 API 与闸门

- [x] 2.1 实现 `GET/PUT /system/team/{teamId}/module-page-settings` 与项目只读 `GET /system/project/{projectId}/module-page-settings`
- [x] 2.2 权限前缀 → 团队开关键映射（`case_plan` 覆盖 case + plan）；实现请求拦截，关闭则 403
- [x] 2.3 MCP / Open API 有效模块与团队闸门求交；项目 MCP 设置 UI 对团队已关模块灰显或提示
- [x] 2.4 `FileUploadService` / 文档上传改为读取「团队有效上限」（空则系统上限）；保存团队上限时校验

## 3. 显示名覆盖

- [x] 3.1 约定并实现前端显示名根键覆盖（菜单、表单标签、常见 `$t`）
- [x] 3.2 后端 MessageSource / 导出表头按团队别名覆盖；MCP 指南可见模块名跟别名
- [x] 3.3 检索并收口硬编码「交付物」等字面量到可覆盖路径（首版覆盖菜单/表单/导出/常见错误）

## 4. 团队设置 UI

- [x] 4.1 枢纽：用例卡改为「用例与计划」并加页面设置链接；新增源码 / 交付物 / 文档设置卡
- [x] 4.2 各卡「页面设置」页（开关 + 名称；文档含上传上限）；缺陷页设置顶部增加显示模块与名称
- [x] 4.3 注册 hidden 路由与 i18n；模块关闭后设置入口仍可供管理员重新开启

## 5. 项目内生效

- [x] 5.1 布局加载团队 `module-page-settings`；侧栏过滤与路由守卫
- [x] 5.2 跨模块字段：`case_plan` off 隐藏缺陷用例相关；`deliverable` off 隐藏交付物选择/依赖编辑
- [x] 5.3 工作台组件声明模块键；关模块时画布与目录不展示对应组件
- [x] 5.4 文档上传组件绑定团队有效 `file-size`

## 6. 验证与文档回写

- [x] 6.1 手工或自动化覆盖：关单模块 / 关 `case_plan` / 全关、别名前后端、上传越界拒存、MCP 求交、工作台隐藏、重开数据仍在
- [x] 6.2 回写长期知识：术语表、数据库、接口清单、产品功能、权限模型；`docs/08-版本记录` 当月一行；新增 `ADR-026-团队功能模块闸门优先于角色菜单与项目MCP` 并登记说明与总览；无单独踩坑则注明无
