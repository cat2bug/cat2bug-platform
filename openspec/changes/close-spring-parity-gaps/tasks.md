## 1. 准备

- [ ] 1.1 确认工作区干净，记录当前 HEAD；从 tag `spring-final-1.1.0` 取出参照：`DataScopeAspect`、`XssFilter` 配置、`OllamaAiServieImpl#init`、`SysCasePromptController#export`
- [ ] 1.2 列出前端全部 `v-html` 与 `innerHTML` 使用点，逐个标注「用户内容 / 受控文案」，确认 `vendor/vue-excel-editor` 的 `cellHtml` 调用方

## 2. 前端 HTML 清洗

- [ ] 2.1 `cat2bug-platform-ui` 加依赖 `dompurify`；新增 `utils/sanitize-html.js`（白名单、`style` 只留颜色与对齐、外链加 `rel`）
- [ ] 2.2 Markdown 渲染组件（`MarkdownContent`、`MarkdownItVueCompat`、`Cat2BugMarkdown`、`Report/ViewReport.vue`）输出经清洗
- [ ] 2.3 评论（`Comment/CommentView`、`Report/ReportDetailComments.vue`）`toHtml` 先转义再格式化再清洗
- [ ] 2.4 `Cat2BugText`、`Cloud/CloudCase`、`Shortcut/CommandPalette.vue` 及 1.2 中其余用户内容点改为清洗后渲染
- [ ] 2.5 单元测试（Vitest 或现有前端测试方式）：脚本、事件属性、`javascript:` 链接被移除，对齐 / 颜色保留；`npm run lint`、`npm run build:prod` 通过
- [ ] 2.6 浏览器验证：缺陷描述、评论、通知、报告各写入一条 XSS 载荷，页面不执行；现有报告样例前后截图一致

## 3. 数据权限

- [ ] 3.1 新增 `DataScopeSqlBuilder`，逻辑对齐若依 `DataScopeAspect#dataScopeFilter`（五种范围、多角色并集、权限字符过滤）；单元测试覆盖五种范围与并集
- [ ] 3.2 用户 / 部门 / 角色列表：服务层先清除外部传入的 `params.dataScope`，非超级管理员时写入生成的片段；`SysUserMapper.xml`、`SysDeptMapper.xml`、`SysRoleMapper.xml` 列表查询拼接 `${params.dataScope}`（别名与 Spring 一致）
- [ ] 3.3 `checkUserDataScope`、`checkDeptDataScope`、`checkRoleDataScope` 在非管理员时真正校验，删除「DataScope AOP not ported」注释
- [ ] 3.4 集成测试：H2 下建两个部门、「本部门」角色用户，验证列表过滤、越权修改被拒、伪造 `params[dataScope]` 无效

## 4. AI 默认模型

- [ ] 4.1 `StartupEvent` 后台检查并拉取默认业务模型（AI 未启用不访问 Ollama；不可达只记警告）
- [ ] 4.2 单元测试：用本地 HTTP 桩模拟 Ollama「模型缺失 / 已存在 / 不可达」三种情况

## 5. 用例提示词导出

- [ ] 5.1 `CasePromptResource` 新增 `POST /system/CasePrompt/export`，列与权限对齐 Spring
- [ ] 5.2 接口测试：有权限返回 xlsx 且行数正确，无权限 403

## 6. 验证

- [ ] 6.1 `unset QUARKUS_CONFIG_LOCATIONS CAT2BUG_INSTALL_CONFIG_PATH && mvn install` 全量测试通过
- [ ] 6.2 重新运行 `/tmp/c2b-parity.py` 接口比对，除有意删除项外缺失为 0
- [ ] 6.3 `deploy/build-native.sh` 重建并跑 `smoke-native-docker.sh arm64` 与 `FRESH=true`，记录体积

## 7. 文档与回写长期知识

- [ ] 7.1 新增 `docs/07-技术决策/ADR-012-用户内容在前端渲染时清洗而非后端转义.md`，加入 ADR 说明与总览
- [ ] 7.2 `docs/03-技术方案/权限模型与鉴权链路.md` 删除 dataScope 已知缺口并写明实现；本机 `CLAUDE.md` 同步
- [ ] 7.3 可复用的坑写入 `docs/06-踩坑记录/`（没有则注明无）；`docs/08-版本记录/2026-10.md` 记录本变更（含「数据范围受限角色列表变少」的行为说明）；`check_links.py` 坏链为 0
