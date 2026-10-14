# ADR-007 文档正文存每项目隐藏 Git 仓库

记录 Markdown/文本文档正文改存「每项目一个隐藏 Git 仓」的决定；注意当前代码状态待确认。

- 日期：2026-07-30
- 状态：已采纳（待确认：当前 `core` 代码中未找到实现，见文末）

## 背景

文档正文原写在 `cat2bug.profile/upload`（`fileUrl`），编辑历史在 `sys_document_history` 存全文快照。项目已支持多 bare 仓与 JGit，希望历史改为 Git 单文件历史，且用户在文档管理中看不到仓库概念。依据：分支 `backup/core-before-rollback-20261014` 中的 `openspec/changes/archive/2026-07-30-document-hidden-git-storage/design.md`，以及 `CLAUDE.md`「文件与 Git 仓库存储」一节。

## 候选方案

### 方案 A：每项目一个独立文档 bare 仓

优点：真正不可 clone、不进源码列表。

缺点：多一类仓库，需要在列表/路由强过滤。

### 方案 B：放在源码仓隐藏目录 `.cat2bug/docs/` + UI 过滤

缺点：clone 仍会泄露文档，否决。

### 方案 C：一文一仓

缺点：仓库数量失控，未采纳。

## 最终决定

- 每项目一个内部文档仓（slug `cat2bug-docs`），复用 `sys_git_repository` 加 `kind` 区分并在列表强过滤，复用 JGit 工具类；不开放 Smart HTTP 与源码浏览。
- 路径扁平 `docs/{docId}.md`（文本按扩展名），显示名与目录树仍在 `sys_document`，改名/移动不打断历史。
- 只在显式保存/创建/恢复时 commit，autosave 不写 Git；commit message 由行级 diff 自动概括。
- 历史 API 仍挂 `/system/document/{docId}/history*`；恢复 = 新 commit；不支持删除单条历史。
- 二进制（pdf/docx 等）仍走 upload。

## 原因

源码仓的 browse / Smart HTTP 无法按路径硬隐藏，只有独立仓才能保证不泄露；扁平路径让历史与 `docId` 稳定绑定。

## 副作用

- 文档仓与源码仓一样落在 `cat2bug.profile/git`，**必须持久化整个 `cat2bug.profile`**，否则数据库记录还在、正文和历史全丢。
- 删除文档后 Git 里仍有 blob。
- 待确认：当前 `core` 未包含 `backup/core-before-rollback-20261014` 的提交（如 `a78e2ed4`、`4315e256`），代码中搜不到 `SysDocumentGitContentStore` / `ISysDocumentGitRepositoryService`，`openspec/specs/` 也没有 `document-hidden-git-storage`，但 `CLAUDE.md` 仍按已实现描述。需确认该能力是否会重新合入、以及 Quarkus 线是否需要实现。
