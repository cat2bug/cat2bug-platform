## Why

项目源码仓库已支持 Smart HTTP 与在线浏览，但分支只能作为代码页下拉切换，无法在平台内管理分支生命周期，也缺少 Pull Request（合并请求）与讨论。团队仍要跳到外部托管做分支整理与合码，与「项目内完成工程协作」的目标不一致。

## What Changes

- 新增仓库级**独立分支页**：默认分支区 + 全部分支列表、按名搜索、最后更新信息、相对默认分支的 ahead/behind
- 分支写操作（新建、删除、设置默认分支）仅 `project.create-by` / `project.admin`（manage）
- 新增 **Pull Request（L2）**：开 PR（与 push 同权）、标题/描述、source→target、提交列表与文件 diff、整单讨论评论、合并与关闭；合 PR 仅 manage
- 分支页提供「创建 Pull Request」入口；PR 有独立列表/详情路由
- 本期不做：我创建的/活跃/非活跃分组、已删除分支回收站、审批流、保护分支规则、CI 门禁、squash/rebase 多策略（合并方式在 design 中固定一种）

## Capabilities

### New Capabilities

- `git-repo-branches`: 仓库级分支列表与管理 API/UI（搜索、最后更新、ahead/behind、manage 写操作）
- `git-repo-pull-requests`: Pull Request 生命周期（创建/列表/详情/diff/讨论/合并/关闭）及权限矩阵

### Modified Capabilities

- `git-repo-browse`: 代码浏览页增加进入独立分支页的导航入口（如分支计数可点）；分支下拉行为本身不变

## Impact

- **后端**：扩展分支元数据接口；分支 Ref 写操作（JGit）；PR/评论表与 Flyway（h2/mysql）；服务端合并写回 target ref；复用现有 pull/push/manage 权限与 commit 身份匹配
- **前端**：仓库级分支页、PR 新建/列表/详情（含讨论）；从 browse 入口跳转
- **非目标（本期）**：行内评论可列为实现阶段可选增强，规格默认要求整单讨论；无 CI、无保护分支、无外部 Git 宿主依赖
