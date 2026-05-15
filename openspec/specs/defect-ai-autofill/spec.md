# defect-ai-autofill Specification

## Purpose
TBD - created by archiving change defect-ai-openai-model-selector. Update Purpose after archive.
## Requirements
### Requirement: 缺陷描述 AI 补全 SHALL 支持用户选择 Ollama 或 OpenAI 模型

缺陷新建与编辑界面中，描述字段所使用的自定义文本框工具区 MUST 提供与「AI 用例生成」一致的模型选择能力（分组展示 Ollama 已下载模型与 OpenAI 账号，并显示当前选中项）。用户 MUST 能在触发 AI 分析前切换模型。触发 AI 分析后，系统 MUST 使用所选模型完成各子能力（标题、交付物、类型、处理人、版本等）的补全请求。

#### Scenario: 用户选择 Ollama 模型后触发分析

- **WHEN** 用户在模型下拉中选择某一 Ollama 模型并触发 AI 分析
- **THEN** 前端向 `/ai/defect/*` 发送的请求体 MUST 携带可识别 Ollama 的 `serviceType` 与 `modelId`（与用例侧 key 规则一致）
- **THEN** 后端 MUST 使用 Ollama 服务实现类对该模型执行生成

#### Scenario: 用户选择 OpenAI 后触发分析

- **WHEN** 用户在模型下拉中选择某一 OpenAI 账号项并触发 AI 分析
- **THEN** 前端请求 MUST 携带 `serviceType` 为 openai 及对应模型/账号标识
- **THEN** 后端 MUST 使用 OpenAI 服务实现类完成生成

### Requirement: 缺陷 AI 请求在未显式指定模型时 SHALL 由服务端选择默认 Ollama 模型

当请求未提供 `modelId`（或等价字段为空）且调用需要模型时，服务端 MUST 使用该项目当前已下载的 Ollama 模型列表中的**第一项**作为默认模型完成推理。服务端 MUST NOT 再依赖数据库中的 `business_module` 列。

#### Scenario: 省略 modelId 调用标题生成

- **WHEN** 客户端发送缺陷 AI 请求且未包含 `modelId`，且项目存在至少一个已下载 Ollama 模型
- **THEN** 服务端 MUST 使用列表排序中的第一个 Ollama 模型完成生成并返回成功结果

### Requirement: 模型选项数据源 SHALL 通过共享接口与 CloudCase 对齐

前端 MUST 通过统一的「项目 AI 模型选项」接口（或经设计确认的统一封装函数）拉取模型列表，供 AI 用例生成与缺陷 AI 共用；CloudCase 与缺陷弹框 MUST NOT 长期维护两套互不一致的拼装逻辑。

#### Scenario: 两处 UI 展示一致分组

- **WHEN** 同一项目下同时打开 AI 用例抽屉与缺陷编辑弹框
- **THEN** 两侧模型下拉中 Ollama/OpenAI 分组与条目来源 MUST 一致（除各自本地缓存策略外）

