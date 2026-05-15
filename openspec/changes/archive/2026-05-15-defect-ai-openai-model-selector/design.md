## Context

- 缺陷 AI：`AiDefectController` 当前固定 `SERVICE_TYPE_OLLAMA`，模型名取自 `SysAiModuleConfig.businessModule`；前端 `AddDefect.vue` / `EditDefectDialog.vue` 在 `Cat2BugTextarea` 工具条放机器人按钮，请求体仅 `projectId` + `describe`。
- 用例 AI：`CloudCase/index.vue` 已维护 Ollama 下载列表 + OpenAI 账号列表、分组、`prompt.modelId` / `serviceType`，并依赖项目配置返回的 `defaultBusinessModel` 等做默认选中。
- 项目 Ollama 页：`ai/index.vue` 表格含「业务识别模型」「图像处理模型」单选列，持久化到 `sys_ai_module_config`（`business_module` / `image_module`）。其中 **`business_module` 被缺陷 AI、用例侧默认逻辑等实际使用**；**`image_module` 与「图像处理模型」UI 当前未接入任何用户可见的图像推理能力**（仅配置存储、Mapper、个别后端如 `OllamaAiServieImpl` 中与默认模型列表相关的辅助逻辑）。删 `image_module` 以收敛表结构与配置页为主，不视为替换既有产品能力。

## Goals / Non-Goals

**Goals:**

- 缺陷 AI 与用例 AI 在**模型来源与通道**上一致：支持 **Ollama 与 OpenAI**，由用户在缺陷弹框中显式选择（下拉展示选中态）。
- **共享**「模型选项拉取 + 分组数据结构」与 CloudCase 对齐，减少重复请求与分叉逻辑。
- 后端 `/ai/defect/*` 根据请求选择服务与模型；**未传模型标识时**，服务端对 Ollama 使用**该项目已下载模型列表的第一项**作为默认（与产品约定）；OpenAI 路径若未传需在设计外与产品确认（建议：未选且存在 OpenAI 账号时是否禁止调用或同样取列表首条）。
- 删除 `sys_ai_module_config` 中 `business_module`、`image_module` 列及相关 UI/接口；清理所有读写与默认注入。

**Non-Goals:**

- **不在本次变更中新增**「按图像处理模型路由」的 AI 能力；`image_module` 删除后若未来要做视觉类能力，应另起需求（显式选模或独立配置）。
- 不在本设计中单方面重新定义 OpenAI 计费/配额策略。

## Decisions

1. **共享 API 形态**  
   - 新增或收敛一个后端接口（例如扩展现有 `SysAiModuleConfigController` 下「模型列表」类接口），一次返回：`{ ollama: [{ key, label }], openai: [{ key, label, accountId? }] }` 或与前端已有结构严格对齐的 DTO，供 `CloudCase` 与缺陷组件共用。  
   - **理由**：避免 `CloudCase` 与缺陷各拼一套 list；前端共享一个 `fetchProjectAiModelOptions(projectId)`（名在设计落地时确定）。

2. **共享 UI**  
   - 抽取 `ModelSelect`（或 mixin/composable + 模板片段）：Element `el-select` + `el-option-group`，与 CloudCase 视觉与数据结构一致；缺陷侧将原 `tools` 槽内机器人改为「下拉 + 触发分析」组合（具体：下拉仅选模型 + 保留主按钮触发，或下拉项内触发——实现阶段按 UX 微调，**默认**为下拉选模 + 旁触发的「分析」与 CloudCase 顶部条一致）。  
   - **理由**：一处改分组/国际化，处处生效。

3. **缺陷请求契约**  
   - 扩展 `AiDescribe`（或新建 VO）：`serviceType`（`ollama` | `openai`）、`modelId`（与 CloudCase 的 `key` 一致，如 `llama3:8b` 或 OpenAI 侧 key）。  
   - `AiDefectController` 各方法：`IAiService service = aiServiceMap.get(serviceType)`；`generate(modelId, ...)` 使用请求内模型；`serviceType` 缺省可视为 `ollama` 以保持兼容；`modelId` 为空则 **服务端**解析项目 Ollama 列表首项。  
   - **理由**：默认策略在服务端统一，防篡改与前端遗漏。

4. **删列与数据**  
   - Flyway：H2 + MySQL 同步迁移 `ALTER TABLE ... DROP COLUMN business_module, image_module`（或分两步）；删除 `SysAiModuleConfig` 字段、Mapper XML、Service 默认值注入、`ai/index.vue` 列与保存 payload。  
   - **理由**：用户明确要求删列；保留列但不用会增加技术债。

5. **CloudCase 默认模型**  
   - 移除对 `defaultBusinessModel` 的依赖后：打开抽屉时 `modelId` 缺省设为 **Ollama 组第一项**；若无 Ollama 则有 OpenAI 第一项。  
   - **理由**：与缺陷侧「首模型默认」一致。

## Risks / Trade-offs

- **[Risk] 遗漏对 `image_module` 的静态引用** → **Mitigation**：全仓 grep 后删改；当前已确认**无业务功能依赖该字段做推理**，风险为编译/映射残留而非用户行为回退。  
- **[Risk] `business_module` 移除后默认/兼容路径未覆盖全入口** → **Mitigation**：重点回归缺陷 AI、用例抽屉、Ollama 列表 API；必要时保留一版「读列忽略」的过渡（若采用分阶段发布）。  
- **[Risk] 迁移后旧库残留列或 Flyway 顺序** → **Mitigation**：新脚本版本号高于现有；在 staging 验证。  
- **[Trade-off] OpenAI 缺陷调用成本** → 与用例一致由用户自选模型承担；权限仍沿用现有 `@PreAuthorize`。  
- **[Risk] 双前端入口行为不一致** → **Mitigation**：共享组件 + 单 API 回归用例（手工清单即可）。

## Migration Plan

1. 部署顺序：先发布**兼容旧列**的后端版本（读列可选、写忽略）若时间紧；用户选择「直接删列」则可单步发布但需停机窗口或确保无旧前端。本变更倾向 **单版本**：后端不再返回/写入列 + 迁移删列 + 新前端一并发布。  
2. 回滚：恢复迁移 down（若生成）或从备份恢复表结构；回滚前端 bundle。

## Open Questions

- 缺陷 AI 在 **仅配置 OpenAI、无 Ollama 模型** 时，`modelId` 为空且 `serviceType` 缺省是否默认走 `openai`？建议产品确认（设计倾向：若请求未带 `serviceType` 且项目无 Ollama 模型，则返回 400 或自动 `openai` 首账号）。  
- `Cat2BugTextarea` 工具条宽度：下拉 + 按钮是否换行，需 UI 确认。
