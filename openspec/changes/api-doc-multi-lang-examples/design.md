## Context

- API 文档位于 `readme/production/api/`，由 `cat2bug-platform-ui/src/views/doc/index.vue` 通过 `markdown-it` 渲染为静态 HTML（`v-html`）。
- 已支持 `::: tip` 容器（`markdown-it-container`）；代码块为无高亮、无行号的 `<pre><code>`。
- 项目已依赖 `highlight.js@10.4.1`（用于代码生成页，文档页未使用）。
- 约 23 个 Open API HTTP 接口分布在 7 个 `api-*.md` 文件中；`api-intro.md` 负责授权与调用约定。

## Goals / Non-Goals

**Goals:**

- 每个 HTTP 接口 `##` 小节末尾展示六语言 Tab 示例（cURL、Java、Python、Node.js、PHP、C#），带语法高亮与行号。
- 示例使用 `${baseUrl}`、`${apiKey}` 占位符；`api-intro.md` 说明如何按部署填写 baseUrl。
- 六语言与接口文档同时发布；提供校验防止缺语言。
- 文档页可选：用户输入 baseUrl/apiKey 后在前端替换当前页示例文本（不修改仓库文件）。

**Non-Goals:**

- 改造为独立 Swagger/OpenAPI UI 或在线 Try-it 代理。
- 修改 Open API 后端 Controller 或鉴权逻辑。
- 为非 HTTP 的说明性章节（如「creator 对象」「状态对照表」）强制添加示例。
- 提供可下载 SDK 包（仅文档内嵌示例代码）。

## Decisions

### 1. 渲染方案：`markdown-it-container` + 静态 HTML Tab

**选择**：新增 `::: code-tabs` 容器，在 `render` 阶段输出带 `data-lang` 的 panel 与按钮条；`loadDoc` / `mounted` 后用轻量原生 JS 绑定 Tab 切换。

**理由**：与现有 `::: tip` 一致；无需在 `v-html` 中挂载 Vue 组件。

**备选**：独立 Vue `<ApiCodeTabs>` 组件 — 需改 Markdown 管线或后处理 AST，成本高。

### 2. 作者语法：容器内多个 fenced code block

```markdown
::: code-tabs
```bash title=cURL
...
```
```java title=Java
...
```
（共 6 个 fence，顺序固定）
:::
```

**选择**：`title=` 映射 Tab 标签；语言 id 用于 highlight.js（`bash`、`java`、`python`、`javascript`、`php`、`csharp`）。

**门禁**：校验脚本检查 6 个 fence 均存在且 title 在允许列表内。

### 3. 高亮与行号

**选择**：`new MarkdownIt({ highlight: (str, lang) => hljs.highlight(...) })`；行号通过 highlight.js 的 line-numbers 插件或对生成 HTML 按行包 `<span class="line">` + CSS `counter`。

**理由**：依赖已存在；与代码生成页技术栈统一。

### 4. 占位符与 baseUrl

**选择**：示例源码统一写 `${baseUrl}`、`${apiKey}`；intro 文档表格列出本地（`:2020`）、Docker（`:8022` 等）、反向代理等典型值。

**可选 UI**：文档顶栏（仅 API 文档路由或全局）两个输入框 +「应用」按钮，替换 `.markdown-body .code-tabs` 内文本节点中的占位符。

**理由**：满足「根据部署而定」且保持 Markdown 可版本管理。

### 5. 各语言栈（与用户决策一致）

| Tab | 实现 |
|-----|------|
| cURL | 命令行 curl |
| Java | `java.net.http.HttpClient`（JDK 11+） |
| Python | `urllib.request` 标准库 |
| Node.js | 全局 `fetch`（文档注明 Node 18+） |
| PHP | `curl_*`（intro 注明需 php-curl） |
| C# | `System.Net.Http.HttpClient` |

### 6. 示例内容生产

**选择**：以 cURL 为事实来源，人工/脚本展开为五语言；`api-file.md` 上传接口单独维护 multipart 模板。

**顺序**：先实现渲染引擎 + `api-intro` 样板 + `api-defect` 中 2～3 个接口验证，再批量补齐其余文件。

### 7. Tab UI 与样式

- 6 个 Tab 横向排列，窄屏 `overflow-x: auto`。
- 激活 Tab 与 GitHub 风格代码块配色协调（沿用/扩展 `markdown.css`）。
- 支持「复制代码」按钮（可选，tasks 中列为 P1）。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 138 段示例与表格参数漂移 | 以现有 md 表格 + Controller 为对照；校验脚本只查结构，人工 review 关键接口 |
| `markdown-it-container` 嵌套 fence 解析错误 | 在 design 样例中单测渲染；文档贡献指南写明 fence 缩进规则 |
| 六 Tab 小屏拥挤 | 横向滚动 + 缩短标签（Node.js → Node） |
| PHP 无 curl 扩展 | intro 明确依赖 |
| Node fetch 版本 | intro 注明 Node 18+ |
| v-html + 内联 JS 安全 | 仅处理本站 md，不执行用户 Markdown |

## Migration Plan

1. 合并文档引擎与样式（向后兼容：无 `code-tabs` 的旧 md 仍正常显示）。
2. 更新 `api-intro.md`（占位符、部署地址、语言环境说明）。
3. 按文件批量追加 `code-tabs`（可单 PR 分文件 review）。
4. 启用 CI/本地 `npm run` 校验脚本。
5. 回滚：还原 md 与 doc/index.vue 即可，无数据迁移。

## Open Questions

- 文档顶栏「baseUrl/apiKey 替换」是否纳入 v1（建议纳入，工作量小）。
- 「复制代码」按钮是否 v1 必做（建议 P1）。
- Python 统一 `urllib.request` 还是允许 `http.client`（建议统一 `urllib.request` 以利模板化）。
