## Why

生成报告时的模版选择器目前是项目库内扁平列表：所有卡片均可编辑/删除，没有国标/QMS 预制分组，skills 里已沉淀的 29119-3 与 9001 模版无法以只读方式进入产品。同时产品文档与历史交付叙述仍大量以 Quarkus 为默认入口，与当前 `core` 线以 Spring Boot admin 为唯一可执行模块的方向不一致，需明确退役 Quarkus 交付并清理残留。

## What Changes

- 报告模版选择 UI **按组展示**：最上「自定义模版」，其次「ISO/IEC/IEEE 29119-3:2021」，再次「ISO 9001」
- 两组标准模版：**不可编辑、不可删除，仅可复制**；复制后写入当前项目的自定义模版（可编可删）
- 预制正文与预览图放入 `cat2bug-platform-admin/src/main/resources/报告/模版/`（classpath），经接口列举/读取；**全量**纳入 skills 中 29119-3 全部 md + 9001 manuals + records
- 用 **HTML→PNG 批生成**预览图，并由 `manifest.json`（或同等配置）声明分组、标题、正文与预览路径
- **退役 Quarkus 交付**：不以 Quarkus 为运行/打包入口；删除或改写仍指向 Quarkus 的模块引用、脚本、文档与 CI；Spring `cat2bug-platform-admin` 为默认后端（本仓库 `core` 上 Quarkus 模块目录若已不存在，则以清残留与改文档为主）

## Capabilities

### New Capabilities

- `report-template-preset-groups`: 分组选择 UI、classpath 预制模版 API、只读/复制语义、manifest + 预览图批生成与资源落盘
- `retire-quarkus-delivery`: 移除 Quarkus 作为产品交付线；文档/脚本/构建默认改为 Spring Boot admin；清理残留引用

### Modified Capabilities

- （无强制改写既有 openspec capability 文本；运行时列表行为由新 capability 约束。既有 `sys_report_template` CRUD 对自定义模版保持不变。）

## Impact

- 前端：`Cat2BugReportTemplateSelect`、模版管理页、键盘导航/分页模型
- 后端：Spring `SysReportTemplateController`（或并列 preset API）、classpath 资源读取；复制预制 → insert 自定义
- 资源：`cat2bug-platform-admin/src/main/resources/报告/模版/**`；skills 为源，resources 为发布副本（可脚本同步）
- 文档：`CLAUDE.md`、`README.md`、`deploy/`、`readme/` 中 Quarkus 默认叙述
- 不向 Quarkus 模块同步资源（交付线退役）
- 预览批生成依赖本机 Node/浏览器或 headless 渲染工具链（design 选定）
