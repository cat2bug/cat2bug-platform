# project-ollama-models Specification

## Purpose
TBD - created by archiving change defect-ai-openai-model-selector. Update Purpose after archive.
## Requirements
### Requirement: 项目 Ollama 模型配置界面 SHALL 不再展示或保存业务/图像默认列

项目设置中「Ollama 模型」页面 MUST 移除「业务识别模型」「图像处理模型」两列及相关单选控件。保存项目模型配置时 MUST NOT 再向服务端提交上述字段。

**说明**：「图像处理模型」及持久化字段 `image_module` 在当前产品中**未接入任何面向用户的图像专用 AI 推理**；移除该列属于配置与数据层收敛，不删除既有用户可见能力。

#### Scenario: 用户仅管理下载与删除

- **WHEN** 用户进入 Ollama 模型配置列表页
- **THEN** 页面 MUST 仅展示与下载、删除、模型名、状态、尺寸等相关的列（以最终实现为准，但 MUST NOT 包含已删除的两列）

### Requirement: 持久层 SHALL 删除 business_module 与 image_module 列

数据库表 `sys_ai_module_config` MUST 通过迁移脚本删除 `business_module` 与 `image_module` 列（H2 与 MySQL 脚本均需提供）。应用实体与 Mapper MUST 同步移除字段映射。

#### Scenario: 迁移后应用启动

- **WHEN** 在已执行迁移的库上启动应用
- **THEN** 应用 MUST 正常启动且不再读写已删除列

### Requirement: 依赖默认业务/图像模型的逻辑 MUST 迁移为运行时默认策略

所有原先读取 `business_module` 作为默认业务模型选择的代码路径 MUST 被替换为：在无显式用户选择时采用**列表首项**或与设计文档一致的统一策略；不得再访问已删除列。对 **`image_module` 的读取** 若仍存在，MUST 随列删除一并移除（当前无产品功能依赖该字段做推理）。

#### Scenario: AI 用例抽屉打开时默认选中

- **WHEN** 用户打开 AI 用例生成抽屉且本地无记住的上次模型
- **THEN** 默认选中的模型 MUST 来自 Ollama 列表首项（若存在），否则为 OpenAI 列表首项（若存在）

