## 1. Backend commit detail API

- [x] 1.1 Add DTOs for commit detail, parent refs, file changes (path, type, stats, binary/truncated, optional patch)
- [x] 1.2 Implement JGit detail+diff against first parent (empty tree for root) with per-file and cumulative patch limits
- [x] 1.3 Expose `GET /system/git/repo/{repositoryId}/commits/{commitId}` with pull checks; not-found for unresolved ids
- [x] 1.4 Add unit/integration tests for text patch, binary/truncated omission, pull denial, unknown commit

## 2. Frontend API and routing

- [x] 2.1 Add `getGitRepoCommit` (or equivalent) in `gitRepo.js`
- [x] 2.2 Register hidden route `.../browse/:repositoryId/commits/:commitId` (e.g. `GitRepoCommitDetail`) with browse breadcrumb parents
- [x] 2.3 Add i18n keys (zh-CN / en-US) for detail labels, truncated/binary placeholders, browse-files

## 3. Commit detail UI

- [x] 3.1 Build commit detail page: subject/body, author avatar, time, parents, stats summary, file list with escaped unified diff
- [x] 3.2 Wire 「浏览文件」 to code browse with `ref=commitId`
- [x] 3.3 Style for light/dark using existing browse/theme tokens

## 4. Navigation entry points

- [x] 4.1 Commits tab: make commit message navigate to detail
- [x] 4.2 Code tab latest-commit bar: make message navigate to detail
- [x] 4.3 Aside recent activity: make message and/or hash navigate to detail

## 5. Verification

- [x] 5.1 Manual: open detail from all three entries; browse-files lands on tree at that commit
- [x] 5.2 Manual: binary or large-change commit shows placeholder without breaking the page
- [x] 5.3 Run relevant backend tests and frontend lint/unit if added
