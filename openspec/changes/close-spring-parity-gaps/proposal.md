## Why

对照 tag `spring-final-1.1.0` 逐接口、逐横切能力比对后，除 IM 外部推送（另见变更 `restore-im-external-push`）外，Quarkus 还缺四项 Spring 行为，其中两项涉及安全：

- **XSS**：Spring 有全局 `XssFilter`（`/system/*`、`/monitor/*`、`/tool/*`，排除通知、评论、报告）转义请求里的 HTML。Quarkus 没有；而前端 Markdown 渲染开启了 `html: true`，评论组件把原文直接拼进 HTML 后 `v-html`。缺陷描述等字段写入 `<script>`、`<img onerror>` 会被原样保存并在其他成员浏览器里执行。评论、通知、报告在 Spring 时期就被排除在过滤之外，同样存在风险。
- **数据权限**：Spring `@DataScope` 按角色数据范围（全部 / 自定义 / 本部门 / 本部门及以下 / 仅本人）过滤用户、部门、角色列表。Quarkus 的 Mapper 没有拼接条件，代码注释写明「DataScope AOP not ported」，数据范围受限的角色能看到全部用户和部门。
- **AI 默认模型**：Spring 启动时若 Ollama 缺少 `cat2bug.ai.default-business-model` 会自动下载；Quarkus 只能手动拉取，新部署首次使用 AI 会失败。
- **用例提示词导出**：`POST /system/CasePrompt/export` 未迁移（前端暂未调用，按对等补齐）。

Spring 线已删除（ADR-010），这些缺口没有回退路径。

## What Changes

- **XSS（前端为主）**：
  - 引入 HTML 清洗库（DOMPurify），在所有渲染用户内容的 `v-html` 处统一清洗：Markdown 渲染输出、评论、`Cat2BugText`、云端用例提示词、命令面板标题等。
  - 评论 `toHtml` 先转义原文再做格式化。
  - 不恢复 Spring 式的后端全局转义（它会破坏 Markdown / JSON，Spring 自己就因此排除了通知、评论、报告），后端不改存储内容。
- **数据权限**：
  - 恢复 `@DataScope` 等价能力：按当前用户角色的数据范围生成 SQL 片段，放入查询参数 `params.dataScope`。
  - 用户、部门、角色三个列表查询的 XML 拼接该片段；超级管理员不受限。
  - `checkUserDataScope`、`checkDeptDataScope`、`checkRoleDataScope` 在非管理员时真正校验。
- **AI 默认模型**：AI 启用且配置了默认业务模型时，启动后异步检查 Ollama，缺失则后台拉取；Ollama 不可达只记日志，不阻塞启动。
- **用例提示词导出**：新增 `POST /system/CasePrompt/export`（FastExcel），列与 Spring 一致。

## Capabilities

### New Capabilities

- `html-render-sanitization`：前端渲染用户内容时的 HTML 清洗规则。
- `role-data-scope`：用户、部门、角色列表按角色数据范围过滤，以及单条操作的数据范围校验。
- `ai-default-model-bootstrap`：启动时确保默认业务模型存在。
- `case-prompt-export`：用例提示词 Excel 导出。

### Modified Capabilities

（无：`openspec/specs/` 中没有上述能力的现有规格。）

## Impact

- **前端**：`cat2bug-platform-ui` 新增依赖 `dompurify`；改 `components/Markdown/*`、`components/Cat2BugMarkdown`、`components/Comment/CommentView`、`components/Report/ReportDetailComments.vue`、`components/Cat2BugText`、`components/Cloud/CloudCase`、`components/Shortcut/CommandPalette.vue` 等 `v-html` 处。`vendor/vue-excel-editor` 只渲染本地化文案和受控单元格，逐个确认后再决定是否处理。
- **后端**：
  - `cat2bug-platform-system`：新增数据范围构造器，改 `SysUserService`、`SysDeptService`、`SysRoleService`。
  - `cat2bug-platform-admin`：改 `SysUserMapper.xml`、`SysDeptMapper.xml`、`SysRoleMapper.xml`、`CasePromptResource`、Ollama 启动逻辑。
- **数据库**：无迁移（`sys_role.data_scope`、`sys_role_dept` 已存在）。
- **行为变化**：数据范围受限的非管理员角色，用户 / 部门 / 角色列表会变少，这是恢复 Spring 行为。
- **文档**：`docs/03-技术方案/权限模型与鉴权链路.md` 删除「已知缺口：Mapper 未拼接 dataScope」；本机 `CLAUDE.md` 同步。
