## 1. 文档渲染引擎（code-tabs）

- [x] 1.1 在 `doc/index.vue` 注册 `markdown-it-container` 的 `code-tabs` 规则，解析容器内带 `title=` 的 fenced code blocks
- [x] 1.2 输出 Tab 按钮条 + 六语言 panel 的 HTML 结构，并在 `loadDoc` 后绑定 Tab 切换逻辑
- [x] 1.3 配置 `markdown-it` 的 `highlight` 回调，注册 bash/java/python/javascript/php/csharp 语言
- [x] 1.4 为 code-tabs 内代码块实现行号显示（highlight.js line-numbers 或等价 CSS/按行 span）
- [x] 1.5 在 `markdown.css`（或 scoped 样式）中补充 code-tabs、Tab 滚动、行号与暗色代码块样式
- [x] 1.6 （P1）文档页增加 `${baseUrl}` / `${apiKey}` 输入与应用替换（仅前端，不改源 md）
- [x] 1.7 （P1）为每个 code panel 增加「复制代码」按钮

## 2. api-intro 与作者规范

- [x] 2.1 更新 `api-intro.md`：部署场景下的 `${baseUrl}` 说明表（本地 :2020、Docker、反向代理等）
- [x] 2.2 更新 `api-intro.md`：六语言环境要求（Node 18+、php-curl、JDK 11+ HttpClient 等）
- [x] 2.3 将 intro 中现有单段 curl 示例改为完整 `::: code-tabs` 六语言样板
- [x] 2.4 在 `readme/production/api/` 或 change 目录添加 `CODE-TABS-AUTHORING.md` 贡献说明（fence 顺序、占位符、禁止缺语言）

## 3. 接口示例内容（六语言齐套）

- [x] 3.1 `api-defect.md`：为全部 HTTP 接口小节末尾添加 code-tabs（约 11 个）
- [x] 3.2 `api-case.md`：为 5 个接口添加 code-tabs
- [x] 3.3 `api-deliverable.md`：为 3 个接口添加 code-tabs
- [x] 3.4 `api-member.md`、`api-project.md`、`api-report-defect.md`：各接口添加 code-tabs
- [x] 3.5 `api-file.md`：上传图片接口六语言 multipart 示例（重点人工校对）

## 4. 校验与验收

- [x] 4.1 新增脚本（如 `cat2bug-platform-ui/scripts/validate-api-doc-examples.cjs`）扫描 api-*.md：每个含 `**方法**`+`**路径**` 的小节须含完整 6 语言 code-tabs
- [x] 4.2 将校验命令接入 `package.json` scripts（可选接入 CI）
- [x] 4.3 浏览器手动验收：Tab 切换、行号、高亮、占位符替换、移动端 Tab 横向滚动
- [x] 4.4 抽查 3 个接口（GET 列表、POST 创建、file 上传）六语言请求与文档表格参数一致
