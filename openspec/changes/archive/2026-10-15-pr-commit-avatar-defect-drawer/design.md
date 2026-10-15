## Context

PR 详情页（`pull-detail.vue`）已有：元数据、提交列表、文件 diff、讨论区（评论带头像 + Markdown）。提交列表只有作者名；描述是 `<pre>` 纯文本。代码浏览与提交详情已用 `memberForAuthor` + `cat2-bug-avatar`；缺陷编辑已有右侧 `EditDefectDialog`（`el-drawer`）。MCP 修复流程会在 PR 标题/正文写 `#编号`，审查人需要不离开 PR 页看缺陷。

## Goals / Non-Goals

**Goals:**

- 提交列表每条显示作者头像（匹配规则与 browse / commit-detail 一致）。
- 标题与描述中的 `#` + 数字（项目内编号）可点，右侧抽屉打开对应缺陷。
- 描述用清洗后的 Markdown 渲染，与讨论区观感一致。

**Non-Goals:**

- 不改后端 PR/缺陷 API，不新增缺陷↔仓库绑定字段。
- 不做行内评论、不做 PR 列表页的 `#编号` 链接（仅详情页）。
- 不做自动部署 / 审验 Agent（另开变更）。
- 不强制把「`#61` + 整段标题」做成单一超链；以 `#编号` 为可点击单元即可（标题旁可同时展示可点的 `#编号`）。

## Decisions

1. **头像：前端匹配，不扩 commits API**  
   复用 `memberForAuthor`（作者名 / 昵称 / 邮箱 → 项目成员列表；匹配不到用文字占位）。项目成员列表在进入仓库壳时已有或按需拉取（与 browse 相同）。  
   *备选*：commits API 直接带 `userId`/`avatar` —— 改动面大，MVP 不做。

2. **缺陷抽屉：复用 `EditDefectDialog`**  
   点击 `#N` → 用当前项目 `projectId` + `projectNum=N` 查缺陷 → 打开现有编辑抽屉。有列表权限即可打开；无权限或查不到时 `$modal.msgWarning`，不跳转。  
   *备选*：只读侧栏 —— 多一套 UI；审查时常需改状态，编辑抽屉更省事。

3. **提及解析：`#` + 1 段以上数字，边界为非数字**  
   正则示例：`/(^|[^A-Za-z0-9_])#(\d+)\b/`。代码块内不解析（Markdown 渲染后再挂链接，或渲染前对非 fence 区域替换为链接节点）。优先与现有 Markdown 渲染组件配合，输出须经 ADR-012 清洗。

4. **标题区**  
   标题仍以文本展示，其中 `#编号` 做成可点链接；不必整行标题都变成链接。

5. **描述渲染**  
   去掉纯文本 `<pre>`，改用与讨论区相同的 `markdown-renderer`（或等价组件），再叠加缺陷提及点击处理。

## Risks / Trade-offs

- [误点代码里的 `#123`] → 仅在标题与 Markdown 正文非代码块区域解析；代码 fence 内保持原文。  
- [编号不存在 / 无权限] → 轻提示，抽屉不打开。  
- [作者邮箱与平台账号不一致] → 与 browse 相同限制，占位头像 + 显示作者名。  
- [EditDefectDialog 较重] → 可接受；若性能差再抽只读侧栏。

## Migration Plan

- 纯前端发布；无 DB / API 迁移。  
- 回滚：还原 `pull-detail.vue` 及相关小组件即可。

## Open Questions

- 无（默认：编辑抽屉 + 与 browse 相同的头像匹配）。

## 相关长期文档

- `docs/04-UI规范/组件与交互约定.md`（抽屉、提示）
- `docs/04-UI规范/主题与配色.md`
- `docs/07-技术决策/ADR-012-用户内容在前端渲染时清洗而非后端转义.md`
- `docs/05-接口文档/接口清单-缺陷与用例计划.md`（`projectNum` 查询）
- 拟新增 ADR：无
