## Context

`git-repo-browse` 已提供分支、目录树、blob 与 `listCommits`（短消息 + 作者 + 时间）。前端「提交记录」Tab、代码页最新提交条、侧栏近期动态均展示提交，但不可进入详情。产品参考 Gitee/GitHub 提交页：全文消息、父提交、文件变更统计与 unified diff。用户已确认：要行级 diff、三处入口都可点、大文件/二进制可截断或跳过；规格挂在 `git-repo-browse` 而非新 capability。

## Goals / Non-Goals

**Goals:**

- 只读提交详情 API（meta + 变更文件 + 受限 patch）
- 独立全宽详情 UI，路由形如 `/project/git-repo/browse/:repositoryId/commits/:commitId`
- 三处入口跳转同一详情
- 「浏览文件」以该 commit 为 ref 回到代码 Tab / 树
- 文档化的 patch 大小与二进制策略，避免 OOM / 超大响应

**Non-Goals:**

- split diff、blame、评论、cherry-pick / revert 等写操作
- PR / Issue 关联
- 含该 commit 的全部分支枚举（可选后续；首版可不做或仅尽力）
- 修改 `listCommits` 列表契约（仍返回 short message）

## Decisions

1. **Capability 归属：扩展 `git-repo-browse`**  
   - 与 branches/tree/blob/commits 同一 pull 只读线，无新表/无 manage CRUD。  
   - 备选：新建 `git-repo-commit-detail` → 拒绝，避免与 browse 重复边界。

2. **API 形状：单端点聚合（首版）**  
   - `GET /system/git/repo/{repositoryId}/commits/{commitId}`  
   - 响应含：id/shortId、fullMessage、author、commitTime、parents[]、stats（files/additions/deletions）、files[]（path、changeType、additions、deletions、binary、truncated、patch?）。  
   - 备选：meta 与 diff 拆两个请求 → 首版合并以减少往返；若实测大仓过慢，再拆 `.../commits/{id}/diff`。

3. **Diff 实现：JGit `DiffFormatter` + `CanonicalTreeParser`**  
   - 与 parent[0] 比（root commit 对 empty tree）；merge commit 首版只对 first parent（与多数托管站默认一致），UI 可注明。  
   - 备选：调用 `git diff` 进程 → 拒绝，保持纯 JGit、Native 友好。

4. **Patch 限制（常量，可配置后续）**  
   - 单文件 patch 上限约 256 KiB；单次响应累计 patch 约 1–2 MiB；超出则 `truncated=true` 且不附完整 patch。  
   - 二进制或不可 UTF-8 文本：`binary=true`，无 patch。  
   - 文件数量可设软上限（如 500），超出仍返回统计但省略后续 patch。

5. **路由与 UI**  
   - 新增 hidden 路由 `GitRepoCommitDetail`，组件可复用 `browse.vue` 壳或独立 `commit-detail.vue`（推荐独立页，全宽更接近参考图；返回链到提交 Tab）。  
   - 备选：像发行版一样嵌在 browse Tab 内只显示一条 → 拒绝，diff 页需要更多纵向空间。

6. **入口**  
   - 提交 Tab 消息、最新提交条消息、侧栏动态消息/hash → 同一 `openCommitDetail(id)`。  
   - Hash 解析：支持完整 id 与仓库内可解析的 abbrev（JGit `resolve`）。

7. **「浏览文件」**  
   - 跳转 `GitRepoBrowse`，query/state 设 `ref=commitId`、`activeTab=code`（与现有 commitish 解析一致）。

## Risks / Trade-offs

- **[Risk] 大仓单次 commit 文件极多 / 大 patch → 内存与响应体膨胀** → Mitigation：硬上限 + truncated/binary 标志；必要时后续拆 diff API 与按 path 懒加载。  
- **[Risk] Merge commit 多父，只 diff first parent 可能误解** → Mitigation：UI 标明对比 parent；后续可加父选择。  
- **[Risk] 前端自研 diff 渲染 XSS** → Mitigation：按行转义纯文本，不用 `v-html` 喂 raw patch。  
- **[Trade-off] 单端点简单但不如按文件懒加载省带宽** → 首版接受；超限截断优先保可用性。

## Migration Plan

- 无 DB 迁移。部署后新旧客户端兼容：仅新增 GET。  
- 回滚：去掉路由与入口点击即可；API 保留无害。

## Open Questions

- 单文件 / 累计 patch 精确字节上限在实现时以常量落地（建议 256 KiB / 1.5 MiB），若产品要改再抽配置。  
- 详情页是否显示 committer（与 author 不同时）：建议显示，实现时带上即可。
