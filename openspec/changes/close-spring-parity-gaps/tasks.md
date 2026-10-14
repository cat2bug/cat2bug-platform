## 1. 准备

- [x] 1.1 确认工作区干净，记录当前 HEAD；从 tag `spring-final-1.1.0` 取出参照：`DataScopeAspect`、`XssFilter` 配置、`OllamaAiServieImpl#init`、`SysCasePromptController#export`
  - HEAD `1dbb658a`。参照中的两处意外：`SysCasePromptController#export` 在 Spring 中整段被注释（见任务 5 说明）；`SysRoleMapper.xml` 的 `${params.dataScope}` 也被注释（见任务 3 说明）
- [x] 1.2 列出前端全部 `v-html` 与 `innerHTML` 使用点，逐个标注「用户内容 / 受控文案」，确认 `vendor/vue-excel-editor` 的 `cellHtml` 调用方（结果见 design.md「v-html / innerHTML 盘点结果」）

## 2. 前端 HTML 清洗

- [x] 2.1 `cat2bug-platform-ui` 加依赖 `dompurify`；新增 `utils/sanitize-html.js`（白名单、`style` 走 CSS 属性白名单（修订，见 design D1）、外链加 `rel`）
- [x] 2.2 Markdown 渲染组件（`MarkdownContent`、`MarkdownItVueCompat`、`Cat2BugMarkdown`、`Report/ViewReport.vue`）输出经清洗（后两者经 `MarkdownItVueCompat` 渲染；图片占位改为捕获阶段 `error` 监听）
- [x] 2.3 评论（`Comment/CommentView`、`Report/ReportDetailComments.vue`）`toHtml` 先转义再格式化再清洗（共用 `utils/comment-html.js`，保留 contenteditable 产生的实体，见 design D1）
- [x] 2.4 `Cat2BugText`、`Cloud/CloudCase`、`Shortcut/CommandPalette.vue` 及 1.2 中其余用户内容点改为清洗后渲染
- [x] 2.5 单元测试（Vitest 或现有前端测试方式）：脚本、事件属性、`javascript:` 链接被移除，对齐 / 颜色保留；`npm run lint`、`npm run build:prod` 通过
  - Jest `tests/unit/utils/sanitize-html.spec.js` 12 例通过，全量 78 套件 448 例通过；`build:prod` 通过
  - `npm run lint` 在本变更前就不可用：`.eslintrc.js` 的 `vue/max-attributes-per-line` 旧写法与 eslint-plugin-vue 9 不兼容，且 `.eslintignore` 的 `*.js`/`*.vue` 实际忽略了全部文件。改用临时修正配置逐文件对比改前改后错误数：未引入新的实质问题（增量仅为沿用文件原有双引号/分号风格）
- [ ] 2.6 浏览器验证：缺陷描述、评论、通知、报告各写入一条 XSS 载荷，页面不执行；现有报告样例前后截图一致
  - 未做：需要完整后端与测试数据，且不能往开发库写数据；以 2.5 的单测（载荷与 markdown-it 实际渲染结果）替代，合并后建议主 agent 在浏览器补一次

## 3. 数据权限

- [x] 3.1 新增 `DataScopeSqlBuilder`，逻辑对齐若依 `DataScopeAspect#dataScopeFilter`（五种范围、多角色并集、权限字符过滤）；单元测试覆盖五种范围与并集（`DataScopeSqlBuilderTest`，13 例；另含 `*:*:*`、无部门、非法别名）
- [x] 3.2 用户 / 部门 / 角色列表：服务层先清除外部传入的 `params.dataScope`，非超级管理员时写入生成的片段；`SysUserMapper.xml`、`SysDeptMapper.xml`、`SysRoleMapper.xml` 列表查询拼接 `${params.dataScope}`（别名与 Spring 一致）——实施偏离：改用 Mapper 独立参数 `${dataScope}`（请求无法注入，无需清除）；角色列表不过滤（Spring XML 中该片段本就被注释），见 design D2
- [x] 3.3 `checkUserDataScope`、`checkDeptDataScope`、`checkRoleDataScope` 在非管理员时真正校验，删除「DataScope AOP not ported」注释（角色只校验存在；补上用户详情与批量删除的校验）
- [x] 3.4 集成测试：H2 下建两个部门、「本部门」角色用户，验证列表过滤、越权修改被拒、伪造 `params[dataScope]` 无效（`QuarkusDataScopeTest`；注：项目 `%test` 配置实际连本机 MySQL 测试库 `cat2bug_platform_test`，不是 H2，测试结束自动删除所建数据）

## 4. AI 默认模型

- [x] 4.1 `StartupEvent` 后台检查并拉取默认业务模型（AI 未启用不访问 Ollama；不可达只记警告）（`OllamaDefaultModelBootstrap`；不带 tag 的模型名同时认 `:latest`）
- [x] 4.2 单元测试：用本地 HTTP 桩模拟 Ollama「模型缺失 / 已存在 / 不可达」三种情况（`OllamaDefaultModelBootstrapTest`，另含 AI 未启用不访问）

## 5. 用例提示词导出

- [x] 5.1 `CasePromptResource` 新增 `POST /system/CasePrompt/export`，列与权限对齐 Spring（Spring 线该方法是注释掉的代码，按注释中的权限 `system:case:add`、实体 `@Excel` 列「提示词内容」「创建人ID」、表名「测试用例提示词数据」实现）
- [x] 5.2 接口测试：有权限返回 xlsx 且行数正确，无权限 403（`QuarkusCasePromptExportTest`；403 与项目其它接口一致，为 HTTP 200 + `code: 403`）

## 6. 验证

- [x] 6.1 `unset QUARKUS_CONFIG_LOCATIONS CAT2BUG_INSTALL_CONFIG_PATH && mvn install` 全量测试通过（worktree 内改用 `mvn verify -Dquarkus.http.test-port=0`，避免写 `~/.m2`：181 个测试，0 失败，2 个跳过）
- [x] 6.2 重新运行 `/tmp/c2b-parity.py` 接口比对，除有意删除项外缺失为 0（以本 worktree 为根运行：`/system/CasePrompt/export` 已不再缺失；剩余 33 条均为有意删除的 `/tool/gen`、`/test/user`、若依 `/index`，或脚本解析误报：`ApiDefectController` 类级路径为常量、Git Smart HTTP 的 `produces/consumes` 被当成路径、`/cat2ai/**` 的 ANY 方法，Quarkus 均有对应资源）
- [ ] 6.3 `deploy/build-native.sh` 重建并跑 `smoke-native-docker.sh arm64` 与 `FRESH=true`，记录体积（合并后由主 agent 执行）

## 7. 文档与回写长期知识

- [x] 7.1 新增 `docs/07-技术决策/ADR-012-用户内容在前端渲染时清洗而非后端转义.md`，加入 ADR 说明与总览
- [x] 7.2 `docs/03-技术方案/权限模型与鉴权链路.md` 删除 dataScope 已知缺口并写明实现；本机 `CLAUDE.md` 同步（`CLAUDE.md` 不入库，按要求本 worktree 不改，由主 agent 合并后删去「已知缺口：Mapper 未拼接 dataScope 数据权限」一句；另同步了 `02-系统架构/整体架构.md` 与两份接口清单）
- [x] 7.3 可复用的坑写入 `docs/06-踩坑记录/`（没有则注明无）；`docs/08-版本记录/2026-10.md` 记录本变更（含「数据范围受限角色列表变少」的行为说明）；`check_links.py` 坏链为 0（新增两篇踩坑；坏链 0）
