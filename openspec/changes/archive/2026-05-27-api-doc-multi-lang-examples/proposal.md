## Why

Open API 文档（`readme/production/api/`）目前以参数表格为主，仅 `api-intro.md` 有一段 cURL 示例，集成方难以快速按所用语言复制调用代码。需要在每个接口小节末尾提供统一、可切换的多语言示例（含行号），并与部署环境的服务地址、项目 API Key 约定一致，降低对接成本。

## What Changes

- 扩展内置文档阅读器（`cat2bug-platform-ui/src/views/doc/index.vue`），支持 `::: code-tabs` Markdown 容器：六语言 Tab（cURL、Java、Python、Node.js、PHP、C#）、语法高亮、行号显示、Tab 切换。
- 在 `api-intro.md` 补充「服务地址（baseUrl）」说明，约定示例占位符 `${baseUrl}`、`${apiKey}`；可选在文档页提供阅读时替换占位符的 UI。
- 为全部 Open API 业务接口（约 23 个，分布于 7 个 `api-*.md`）在各自 `##` 接口小节末尾添加完整的六语言 `::: code-tabs` 示例块；六语言须同时交付，不允许仅 cURL。
- 约定各语言实现：Java 使用 JDK 自带 `HttpClient`；Node.js 使用全局 `fetch`（Node 18+）；Python 使用标准库；PHP 使用 `curl` 扩展；C# 使用 `HttpClient`。
- 增加校验（脚本或 CI）确保每个 HTTP 接口小节均包含 6 个语言的 code-tabs。

## Capabilities

### New Capabilities

- `api-doc-code-tabs`: 文档 Markdown 中 `code-tabs` 容器的语法、渲染行为（Tab、高亮、行号、占位符替换）及作者规范。
- `api-doc-examples`: Open API 各接口文档末尾六语言调用示例的内容要求、占位符、语言栈与完整性门禁。

### Modified Capabilities

（无。本次为文档与文档 UI 能力新增，不改变现有 `openspec/specs/` 中业务域规范。）

## Impact

- **前端**：`cat2bug-platform-ui/src/views/doc/index.vue`、`markdown.css`（或等价样式）；可能引入/复用 `highlight.js`。
- **文档**：`readme/production/api/*.md`（8 个文件，其中 7 个为接口文档）。
- **工具**：可选 `scripts/` 下示例校验或生成脚本。
- **无后端 API 行为变更**；Open API Controller 逻辑不变。
