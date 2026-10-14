## ADDED Requirements

### Requirement: 启动时确保默认业务模型存在
系统 SHALL 在 AI 已启用且配置了 `cat2bug.ai.default-business-model` 时，于启动后在后台检查 Ollama 的模型列表；缺少该模型时 SHALL 发起后台拉取。检查与拉取 MUST NOT 阻塞应用启动。

#### Scenario: 模型缺失
- **WHEN** AI 已启用、默认模型为 `qwen2.5:7b`，Ollama 中没有该模型
- **THEN** 应用正常启动，并在后台开始拉取 `qwen2.5:7b`

#### Scenario: Ollama 不可达
- **WHEN** AI 已启用但 Ollama 服务不可达
- **THEN** 应用正常启动，日志记录一条警告，不重试

#### Scenario: AI 未启用
- **WHEN** `cat2bug.ai.enabled` 为假
- **THEN** 启动时不访问 Ollama
