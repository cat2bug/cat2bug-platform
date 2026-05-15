## 1. 盘点与迁移准备

- [x] 1.1 全仓检索 `business_module`、`image_module`、`defaultBusinessModel`、`getBusinessModule` 等引用，输出影响清单；**其中 `image_module` 预期仅为配置/持久化/辅助代码，无用户可见推理链路**，以删列与去映射为主，`business_module` 相关路径需重点验证替换为「列表首项」等策略
- [x] 1.2 确认 `sys_ai_module_config` 当前数据与备份策略，评审 Flyway 版本号（H2 / MySQL）

## 2. 数据库与领域模型

- [x] 2.1 新增 Flyway：删除 `sys_ai_module_config.business_module`、`image_module` 列（H2 + MySQL）
- [x] 2.2 更新 `SysAiModuleConfig` 实体、`SysAiModuleConfigMapper.xml`、Service/Controller 中所有字段映射与 SQL 片段
- [x] 2.3 移除 `SysAiModuleConfigServiceImpl` 等对 yml 默认 business/image 的写入逻辑（或改为无列后的 no-op）

## 3. 后端 API

- [x] 3.1 设计并实现「项目 AI 模型选项」统一接口（或扩展现有接口），返回供前端分组渲染的结构（Ollama 已下载列表 + OpenAI 账号列表）
- [x] 3.2 扩展缺陷 AI 请求 VO（如 `AiDescribe`）：`serviceType`、`modelId`；补充校验与文档注释
- [x] 3.3 重构 `AiDefectController`（及关联私有方法）：按 `serviceType` 选择 `IAiService`；`modelId` 为空时解析项目 Ollama 首模型；OpenAI 分支与用例侧参数对齐
- [x] 3.4 调整/删除 `SysAiModuleConfigController` 等仍向下游暴露 `businessModule`/`imageModule` 的字段；更新 `OllamaAiServieImpl` 等依赖

## 4. 前端共享与 CloudCase

- [x] 4.1 抽取模型选择器（组件或 composable），消费统一 API；样式与分组与 `CloudCase/index.vue` 现行为对齐
- [x] 4.2 改造 `CloudCase/index.vue`：改用共享模块与接口；移除对 `defaultBusinessModel` 的依赖，默认选中逻辑改为「Ollama 首项 / 否则 OpenAI 首项」及本地缓存策略（与设计一致）
- [x] 4.3 改造 `views/system/project/ai/index.vue`：删除两列与相关保存字段

## 5. 前端缺陷

- [x] 5.1 改造 `AddDefect.vue`、`EditDefectDialog.vue`：描述区工具条接入共享模型选择器；触发 AI 时在请求中带 `serviceType` 与 `modelId`
- [x] 5.2 联调 `/ai/defect/*`：覆盖 Ollama、OpenAI、缺省 modelId 三种路径（缺省路径由后端 `AiInferenceModelResolver` 在请求未带 `modelId` 时解析；前端已强制选择模型后再调用）

## 6. 文档与验收

- [x] 6.1 更新 `readme/production`：同步删除「业务识别模型」「图像处理模型」配置说明；可注明**当前版本无独立「图像模型」推理能力**，避免读者误以为删列会关闭在用功能
- [x] 6.2 手工验收清单（请在发布前自测）：项目无模型、仅 Ollama、仅 OpenAI、双栈并存；迁移后冷启动；缺陷/用例模型下拉与本地缓存；OpenAI 账号列表与权限（`system:defect:add` / `system:case:add` 可调 `GET /system/ai/project-model-options`）
