## Why

缺陷描述 AI 补全目前仅绑定 Ollama，且模型依赖项目里「业务识别模型」等列配置，与「AI 用例生成」中已支持的 Ollama/OpenAI 双栈及显式选模能力不一致；同时项目 Ollama 配置页维护两套默认列增加心智负担——其中**「图像处理模型」列与 `image_module` 字段当前未用于任何面向用户的推理能力**，可与「业务识别」一并删除以收敛配置与表结构。需要在缺陷侧对齐用例侧的模型选择与后端能力，并简化项目级模型配置（删列 + 数据层收敛），未显式选模时由系统按「首个可用模型」回退。

## What Changes

- 缺陷新建/编辑中，描述区工具条由单一机器人按钮改为**与 AI 用例生成一致的模型下拉**（分组、选中态），用户可选 Ollama 模型或 OpenAI 账号后再触发 AI 分析。
- **缺陷 AI 后端**（`/ai/defect/*`）支持按请求选择 **Ollama 或 OpenAI** 调用对应 `IAiService`，与用例侧语义对齐；请求体扩展模型标识与服务类型；**未传模型时服务端使用该项目下 Ollama 已下载列表的第一个模型**（与产品约定一致）。
- **抽取共享能力**：模型选项拉取与分组逻辑与 `CloudCase` 对齐——抽「模型选择器」组件或 composable，并暴露/复用**共享的配置拉取 API**（避免两处各维护一套 list 拼装）。
- **Ollama 模型配置页**：移除「业务识别模型」「图像处理模型」两列及相关保存逻辑；**数据库删除** `business_module`、`image_module` 列（及 Flyway 迁移）；清理后端读写、默认值注入（如 `SysAiModuleConfigServiceImpl` 中基于 yml 写入默认列）、以及仍依赖这两列的其他调用点（含 `CloudCase` 内 `defaultBusinessModel` 等默认选中逻辑，改为「列表首项」或等价策略）。  
  **说明**：截至本变更立项时，**「图像处理模型」/ `image_module` 未接入任何面向用户的图像专用 AI 推理流程**（仅存配置页、持久化与少量后端辅助代码）；删列以去冗余为主，回归重心在 **`business_module` 与默认业务模型** 的替换路径。
- **BREAKING**：删除 `sys_ai_module_config` 中列后，旧客户端若仍提交该字段需忽略；对外 API 契约若曾返回该字段需版本说明或同步前端。

## Capabilities

### New Capabilities

- `defect-ai-autofill`：缺陷描述 AI 补全的交互（模型下拉）、请求参数（含 OpenAI）、后端路由到正确服务、无模型时的默认策略说明。
- `project-ollama-models`：项目 Ollama 模型列表 UI 简化（删列）、持久层与迁移、相关服务与用例侧默认模型策略调整。

### Modified Capabilities

- （仓库 `openspec/specs/` 当前无既有能力文档；若后续在根 specs 增加基线，可再补 delta。）

## Impact

- **前端**：`AddDefect.vue`、`EditDefectDialog.vue`、`CloudCase/index.vue`、`views/system/project/ai/index.vue`；新建共享模块（如 `components/AI/ModelSelect` 或 `api/ai/projectModelOptions.js` 等，具体名在设计中定）。
- **后端**：`AiDefectController` 及 `AiDescribe`（或等价 VO）、`SysAiModuleConfig` 实体与 Mapper、`SysAiModuleConfigServiceImpl`、`SysAiModuleConfigController`、`OllamaAiServieImpl` 等引用 `business_module`/`image_module` 处；可能涉及 `SysAiModuleConfigController` 返回给前端的模型列表接口形态。
- **数据库**：`sys_ai_module_config` 表结构变更（Flyway H2/MySQL）；数据迁移或丢弃列上前提确认。
- **文档**：`readme/production` 中与「业务识别模型」「图像处理模型」相关的用户指南需同步更新（后者可注明已移除且当前无独立图像模型能力）。
