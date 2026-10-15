## Why

AI 修复缺陷后会在平台仓库提 PR，描述里常写 `#61`。当前 PR 详情页提交列表只有作者文字、没有头像；标题与正文是纯文本，无法从 `#编号` 跳到缺陷，审查人要反复切页核对。MCP 修复闭环已经跑通，补上这两处交互，审查体验才能跟上。

## What Changes

- PR 详情「提交」列表：每条提交显示作者头像（与代码浏览 / 提交详情同一套成员匹配规则：昵称 / 用户名 / 邮箱 → 项目成员；匹配不到则用文字占位头像）。
- PR 标题与描述中的缺陷引用（`#` + 项目内编号）可点击；点击后从**右侧抽屉**打开该缺陷详情（复用现有缺陷编辑抽屉或等价只读侧栏），不离开 PR 页。
- 描述由纯文本 `<pre>` 改为可渲染 Markdown（清洗后），以便链接与既有评论区一致；无法解析或不存在的编号保持原文、不报错打断阅读。
- 无 **BREAKING** 变更；不改 PR / 缺陷后端接口契约（抽屉用现有按 `projectNum` 查询能力）。

## Capabilities

### New Capabilities

- `pr-defect-mention-drawer`：PR 详情中识别 `#编号`，点击后右侧展开缺陷详情抽屉。

### Modified Capabilities

- `git-repo-pull-requests`：PR 详情提交列表须展示提交人（作者）头像；PR 描述展示方式须支持可点击的缺陷引用（与上一能力配合）。

## Impact

- 前端：`cat2bug-platform-ui/src/views/system/git/repo/pull-detail.vue`；复用 `cat2-bug-avatar`、`memberForAuthor` 模式（见 `browse.vue` / `commit-detail.vue`）、`EditDefectDialog`（或抽出的只读侧栏）、Markdown 渲染与 [[ADR-012]] 清洗。
- 后端：原则上不新增接口；缺陷加载沿用 `GET /system/defect/list?projectId=&projectNum=` 或已有详情接口。
- 文档：归档前回写 `docs/04-UI规范/组件与交互约定.md`（缺陷提及链接约定）与当月版本记录；不新开 ADR（沿用现有头像匹配与内容清洗决策）。
- 测试：前端组件 / e2e 或手工验收清单；不强制改 MCP。
