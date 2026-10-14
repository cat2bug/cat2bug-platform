## Context

- 比对依据：tag `spring-final-1.1.0` 的 512 个 HTTP 接口与 Quarkus 资源逐一比对，除有意删除的 `/tool/gen`、`/test/user` 外只差 `/system/CasePrompt/export`；`/cat2ai/**` 在 Spring 接受任意方法，Quarkus 只接 POST，前端也只用 POST，不处理。
- 横切能力比对：`@Log` 已对等；`@RateLimiter`、`@RepeatSubmit`、`@DataSource` 在 Spring 中没有业务使用；`XssFilter`、`@DataScope` 缺失；Ollama 启动拉模型缺失。
- 前端现状：`components/Markdown/MarkdownContent.vue`、`MarkdownItVueCompat.vue`、`components/Cat2BugMarkdown`、`components/Report/ViewReport.vue` 中 `markdown-it` 均为 `html: true`；`Comment/CommentView` 与 `Report/ReportDetailComments.vue` 的 `toHtml` 直接拼接原文。

## Goals / Non-Goals

**Goals:**

- 用户输入在任何页面都不能执行脚本或注入事件属性。
- 数据范围过滤与 Spring（若依 `DataScopeAspect`）结果一致。
- 新部署开启 AI 后不需要手动拉默认模型。
- 补齐唯一缺失的业务接口。

**Non-Goals:**

- 不清洗已存储的历史数据（渲染时清洗已足够）。
- 不恢复后端全局 XSS 转义过滤器。
- 不把数据范围扩展到缺陷、用例等业务表（Spring 也没有）。
- 不处理 `/cat2ai` 非 POST 方法。

## Decisions

### D1 XSS 在渲染端清洗，不在后端转义

- 新增 `utils/sanitize-html.js`：封装 DOMPurify（独立实例，不污染全局），统一白名单（允许 Markdown 常见标签、`img` 的 `src`/`alt`、`a` 的 `href`/`title`/`target`，禁止 `script`、`style`、`iframe`、`object`、`embed`、`form` 等元素、`on*` 属性、`javascript:`/`vbscript:` 及非图片 `data:` 链接；`target=_blank` 自动加 `rel="noopener noreferrer"`）。另提供 `escapeHtml`、`textToHtml`（纯文本转义后换行变 `<br />`）。
- **内联样式按 CSS 属性白名单过滤**（实施时修订）：原计划只留 `color`/`background-color`/`text-align`，但报告图片序列化为 `<img style="width;height;object-fit;transform: rotate()">`，DSL 插件（居中、用例卡片）用 `display`/`flex`/`padding`/`border`/`vertical-align` 等，只留三项会破坏现有报告。白名单为颜色、对齐、字体、尺寸、内外边距、边框、`display`/flex 布局、`object-fit`、`float` 等；值含 `url(`、`expression(`、`javascript:`、`@import`、`\`、`<`、`>` 的声明丢弃；`transform` 只允许 `rotate(...deg)`；不允许 `position`。
- Markdown 组件保留 `html: true`（报告模版与已有内容依赖内联 HTML），渲染结果统一经过清洗再交给 `v-html`。`ImagePlugin` 生成的 `onerror` 占位图脚本会被清洗掉，改为在渲染容器上用捕获阶段的 `error` 监听（`handleImageErrorEvent`）回退占位图。
- 评论 `toHtml`（`utils/comment-html.js`，实施时修订）：评论输入框是 contenteditable，提交的是 `innerHTML`（已含 `&lt;`、`&nbsp;` 等实体，换行可能是 `<div>`/`<br>`）。若对原文整体转义，历史评论会显示出字面量 `&nbsp;`。因此先把无属性的 `<div>`/`</div>`/`<br>` 规整为换行，再转义 `<`、`>`、`"` 和「不构成实体的 `&`」，保留已有实体；然后按行包装、替换 `[img:名称]` 表情，最后清洗。直接调 API 写入的原始 `<script>` 会以文本显示。
- 纯文本字段（`Cat2BugText`、云端用例提示词、命令面板标题、导入结果弹窗、新通知提醒、Excel 滚动提示）转义后再拼接 HTML；`utils/index.js#html2Text` 改用 `DOMParser` 惰性解析（当前文档的游离节点仍会加载 `<img>` 并触发 `onerror`）。
- **备选 A**：恢复后端 `XssFilter`。否决：会把 Markdown、报告 DSL、JSON 中的引号转义坏掉，Spring 正是因此排除了通知、评论、报告，留下了缺口。
- **备选 B**：把 `markdown-it` 改为 `html: false`。否决：会破坏报告模版与已保存内容中的内联 HTML（对齐、颜色等）。

### D2 数据范围：服务层生成片段，XML 拼接

- 新增 `DataScopeSqlBuilder`（system 模块），输入当前用户、部门别名、用户别名、权限字符，输出与若依 `DataScopeAspect#dataScopeFilter` 相同的 SQL 片段（`AND (...)`），只拼接数字 ID 与固定别名，不拼接用户输入。
- 在 `SysUserService` / `SysDeptService` / `SysRoleService` 的列表方法里，非超级管理员时把片段放入 `params.dataScope`；XML 列表查询末尾加 `${params.dataScope}`（与若依写法一致）。
- `check*DataScope`：非管理员时按带数据范围的列表查询目标 ID，查不到就抛「没有权限访问数据」。
- **备选**：MyBatis 拦截器自动改写 SQL。否决：Quarkus MyBatis 拦截器在 Native 下需额外注册，且改写 SQL 不透明；显式 `${params.dataScope}` 与 Spring 完全一致、易审查。

### D3 AI 默认模型：启动后异步拉取

- 监听 `StartupEvent`：`cat2bug.ai.enabled` 为真且 `cat2bug.ai.default-business-model` 非空时，在后台线程调用 `OllamaModelService` 查询模型列表，缺失则调用已有的 `pullModule(model, true)`。
- Ollama 不可达或拉取失败只记 `WARN`，不重试、不阻塞启动。

### D4 用例提示词导出

- 按 Spring `SysCasePromptController#export` 的列与权限（`system:prompt:export` 或对应权限字符，以 Spring 代码为准）实现，使用现有 FastExcel 导出工具。

## Risks / Trade-offs

- [清洗白名单过严导致现有报告 / 文档样式丢失] → `style` 走 CSS 属性白名单（见 D1，覆盖报告图片尺寸 / 旋转与 DSL 插件布局），单测覆盖颜色、对齐、图片尺寸与旋转保留；用现有报告样例做前后截图对比。
- [数据范围恢复后，部分用户「看不到人」] → 这是恢复 Spring 行为；在版本记录中写明，超级管理员不受影响。
- [`${params.dataScope}` 是字符串拼接] → 片段只由服务端用数字 ID 和常量别名生成，外部传入的 `params.dataScope` 必须在进入服务前清除（Controller 层统一覆盖），单元测试覆盖「请求参数伪造 dataScope 被忽略」。
- [Native 下新增代码] → 实现后跑 Native 冒烟。

## Migration Plan

- 无数据迁移。前端清洗与后端数据范围同版本发布；回滚即回退提交。

## Open Questions

- ~~`vendor/vue-excel-editor` 的 `cellHtml` 是否会渲染缺陷 / 用例里的用户字段~~：已核实。唯一调用方 `views/system/defect/list/excel.vue` 的图片/附件列与表头标签都已对路径、字段名做属性转义，属受控 HTML；但组件自带的纵向滚动提示 `vScroller.runner` 会把列名与单元格原值拼进 `v-html`，已改为转义。

### v-html / innerHTML 盘点结果（任务 1.2）

- 用户内容（已处理）：`Markdown/MarkdownContent`（缺陷描述、通知）、`Markdown/MarkdownItVueCompat`（报告、`Cat2BugMarkdown` 预览、Git README / PR / 发行版）、`Comment/CommentView`、`Report/ReportDetailComments`、`Cat2BugText`、`Cloud/CloudCase`（提示词列表、历史需求）、`Shortcut/CommandPalette`、`views/tool/project/browser/BrowserInput`、`vendor/vue-excel-editor` 滚动提示、`layout/components/Navbar` 新通知提醒（`dangerouslyUseHTMLString`）、用户/缺陷/用例导入结果弹窗、注册成功弹窗、`utils/index.js#html2Text`。
- 受控文案（不处理）：`vendor/vue-excel-editor` 的本地化标签与 `headerLabel`、`excel.vue` 的 `cellHtml` 与拖曳幽灵（均已转义）、`views/member/keyboard` 的 i18n 说明、`views/tool/gen` 的 highlight.js 输出、`PersonalRemindTimer` 倒计时数字、`report-block-add-overlay` 菜单、表格单元格工具栏 SVG、`capture-template-preview`（自带转义）、报告导出（`report-export-surface`、`report-word-export` 读取的是已清洗的预览 DOM）、`CommentInput` 自身的 contenteditable。

## 相关长期文档

- `docs/03-技术方案/权限模型与鉴权链路.md`（数据权限已知缺口）
- `docs/03-技术方案/报告模版DSL与Milkdown编辑器.md`（报告内联 HTML 依赖）
- `docs/02-系统架构/整体架构.md`
- `docs/07-技术决策/ADR-010-移除Spring后端并统一模块命名.md`
- 拟新增 ADR：`ADR-012-用户内容在前端渲染时清洗而非后端转义`（以后新增任何富文本展示都受此约束）
