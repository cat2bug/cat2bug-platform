## Context

团队设置枢纽现为四卡（信息 / 用例 / 缺陷 / 成员）。缺陷已有「页面设置」（`sys_team.defect_page_config`：视图 / 色阶 / 分享）。功能模块显隐只靠角色菜单与项目 MCP（ADR-014），无团队硬闸门；产品文案靠固定 i18n，无法按团队改「交付物」等称呼；文档上传上限硬编码于 `FileUploadService`（500MB），无系统可配上限供团队覆盖。

## Goals / Non-Goals

**Goals:**

- 枢纽补齐源码 / 用例与计划 / 交付物 / 文档设置卡，各卡含「页面设置」；缺陷卡补「显示模块 + 模块名称」
- 团队级功能模块硬闸门：关则该团队下所有项目 UI、API、跨模块关联字段、工作台组件、MCP 有效模块一并失效
- 用例与计划：**一个开关**、**两个显示名**
- 允许全部业务模块关闭；数据不级联删除
- 模块显示名覆盖前后端给人看的文案；标识符不变
- 系统可配上传上限；团队文档上限 ≤ 系统上限

**Non-Goals:**

- 项目级覆盖团队模块开关 / 别名
- 改 URL 段、`sys_module` 表名、权限串、Open API `deliverable`、MCP 模块键
- 改缺陷视图色阶/分享算法本身（仍由 `defect_page_config` 管）
- 关模块时清空历史业务数据
- 推翻 ADR-014（仅在其上增加团队闸门求交）

## Decisions

### D1. 统一 `sys_team.module_page_config` JSON

- **选择**：新增一列存各功能模块的 `enabled`、`label`（及文档 `maxUploadMb`）；缺陷专有视图/色阶/分享仍在 `defect_page_config`。
- **模块键（团队开关键）**：

| 键 | 闸门映射到的能力 |
|----|------------------|
| `case_plan` | `case` + `plan`（含 PlanItem、case-review 等） |
| `defect` | 缺陷业务与分享 |
| `deliverable` | 交付物（权限前缀仍 `system:module:`） |
| `document` | 文档 |
| `code` | 源码 Git |

- **默认**：全部 `enabled: true`；`label` 空表示用默认 i18n；`document.maxUploadMb` 空表示用系统上限（或与系统相同的产品默认）。
- **别名**：`case_plan` 存 `labelCase` / `labelPlan` 两个字段；其余模块一个 `label`。
- **备选**：每模块一列 → 否决（读拦截器与项目 bootstrap 一次取出更合适）。

示例：

```json
{
  "case_plan": { "enabled": true, "labelCase": null, "labelPlan": null },
  "defect": { "enabled": true, "label": null },
  "deliverable": { "enabled": true, "label": "模块" },
  "document": { "enabled": true, "label": null, "maxUploadMb": 200 },
  "code": { "enabled": true, "label": null }
}
```

### D2. 枢纽与入口

- 新增卡片：源码设置、交付物设置、文档设置；用例卡改名为「用例与计划设置」，保留类型/模板链接并加「页面设置」。
- 各卡「页面设置」→ hidden 路由（`activeMenu` 指向团队设置），与缺陷页设置同模式。
- 缺陷「显示模块 / 名称」做在现有 `team-defect-page-settings` 页顶部（读写 `module_page_config.defect`），下方仍为视图/色阶/分享。
- 模块关闭后：团队管理员仍可进对应设置卡与页面设置以便重新开启；业务属性/模板等配置页仍可打开（预配置），但项目内业务入口与 API 仍拒。

### D3. 硬闸门生效面

```
有效能力 = 角色菜单/权限 ∩（MCP 时再 ∩ 项目 MCP 勾选）∩ 团队 module_page_config.enabled
```

1. **前端**：当前团队配置注入布局；过滤侧栏；路由守卫；缺陷等表单/筛选项按关联模块隐藏；工作台按组件声明的模块键卸载。
2. **后端**：统一过滤器/拦截器：由请求权限前缀或资源路径映射到团队模块键（`case_plan` 同时覆盖 case 与 plan 前缀）；关闭则 403，业务错误码/文案表明模块未启用。Open API 与 MCP 回环同样走求交。
3. **跨模块字段**：`case_plan` off → 缺陷不展示测试用例相关属性/筛选；`deliverable` off → 各处交付物选择器与依赖字段隐藏（已有值只读展示策略：隐藏控件即可，列表单元格可显示历史名但不提供编辑入口）。
4. **工作台**：组件注册表增加 `teamModuleKey`（或等价）；`enabled=false` 时画布实例与目录分组均不展示。

### D4. 显示名覆盖

- 约定各模块「显示名根键」清单（如前端 `module` / `case` / `plan` / `doc` / `defect.manage`；后端 `messages*.properties` 对应键）。
- 前端：团队配置加载后合并进 i18n 或提供 `teamModuleLabel(key)` 包装 `$t`。
- 后端：带团队上下文的 `MessageSource` 访问点覆盖根键；Excel 导出表头、校验消息走同一路径。
- MCP 指南拼接时用别名替换可见中文模块名。
- **不改**：路径、JSON 字段名、权限串、MCP key。

### D5. 上传上限

- 系统后台新增配置项（`sys_config`）表示全局最大上传 MB；缺省与现网 `FileUploadService` 500 对齐；HTTP `max-body-size` 仍可更大（大文件同步等），业务上传以配置为准。
- 团队 `document.maxUploadMb` 保存时校验 `1 ≤ value ≤ 系统上限`；空则用系统上限。
- 文档上传前端 `:file-size` 与后端校验都读「团队有效上限」。

### D6. API

- `GET/PUT /system/team/{teamId}/module-page-settings`：读写整份 `module_page_config`；权限与团队设置管理员对齐（可复用团队 edit 或按卡拆权限种子；首版优先与现有团队配置管理员集合一致，避免过多新 perms）。
- `GET /system/project/{projectId}/module-page-settings`：项目成员只读，供布局 / 缺陷页 / 工作台 bootstrap。
- 文档上传与通用 upload 在解析到项目/团队时套用有效上限。

### D7. 与 MCP

- 有效 MCP 模块列表 = `normalize(项目勾选)` 去掉团队关闭项；`case_plan` off 则同时去掉 `case` 与 `plan`。
- 项目 MCP 设置 UI：团队已关的模块灰显或不可勾选并注明「团队已关闭」。

### D8. 拟新增 ADR

- **ADR-026（拟定）团队功能模块闸门优先于角色菜单与项目 MCP**：说明 `module_page_config` 为硬闸门、与 ADR-014 求交关系、标识符与显示名分离。

## Risks / Trade-offs

- [漏拦 API] → 以权限前缀表为单一映射源（对齐 `McpModules` 并扩展 `case_plan`）；单测覆盖每前缀  
- [别名漏网（硬编码中文）] → 根键清单 + 代码检索「交付物」等字面量逐步收口；首版保证菜单/表单标签/导出/常见错误  
- [全关后用户迷路] → 工作台空态与模块未启用提示指向团队设置（仅管理员可见入口文案）  
- [关交付物后用例必填 moduleId] → `case_plan` 仍开但 `deliverable` 关时：用例/计划创建校验与选择器策略在实现任务中显式处理（隐藏必填或改为可选并拒新建依赖）；首版优先「隐藏并禁止新建/编辑依赖交付物的字段」  
- [拦截器性能] → 团队配置按 teamId 短缓存，写时失效  

## Migration Plan

1. Flyway：`sys_team.module_page_config`；`sys_config` 插入上传上限；存量 `NULL` → 读时视为全开默认  
2. 部署后端闸门与 API，再发前端枢纽与守卫  
3. 回滚：忽略新列则闸门失效（全开）；前端忽略只读字段则无别名/无新卡  

## Open Questions

- 无（开关绑死、两显示名、可全关、工作台隐藏、前后端改名、缺陷可关均已拍板）

## 相关长期文档

- `docs/00-项目总览.md`
- `docs/01-产品需求/术语表.md`
- `docs/01-产品需求/产品定位与核心功能.md`
- `docs/02-系统架构/数据库.md`
- `docs/03-技术方案/权限模型与鉴权链路.md`
- `docs/04-UI规范/组件与交互约定.md`
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`
- `docs/07-技术决策/ADR-014-MCP权限为角色权限与项目模块的交集.md`
- `docs/07-技术决策/ADR-023-用例类型评审模板与缺陷属性归属团队.md`
- `docs/07-技术决策/ADR-025-Flyway迁移版本与产品版本对齐.md`
- `docs/07-技术决策/说明.md`
- 拟归档回写：术语表、数据库、接口清单、产品功能、权限模型；版本记录当月一行
- 拟新增 ADR：`ADR-026-团队功能模块闸门优先于角色菜单与项目MCP`
