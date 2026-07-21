## 1. Branch enrichment backend

- [x] 1.1 Extend `GitBranchInfo` (or DTO) with tip author/time/shortId and ahead/behind vs default
- [x] 1.2 Implement JGit tip summary + ahead/behind counting in browse/branch service; support optional name search filter
- [x] 1.3 Keep `GET .../branches` pull-gated; add tests for empty repo, default flag, ahead/behind

## 2. Branch write APIs

- [x] 2.1 Implement create branch (from ref, default = default branch tip) via JGit `RefUpdate`; manage-only
- [x] 2.2 Implement delete branch (reject default); manage-only
- [x] 2.3 Implement set default branch (persist `sys_git_repository.default_branch`, validate ref exists); manage-only
- [x] 2.4 Expose REST + permission tests (develop denied, admin allowed)

## 3. Branches page UI

- [x] 3.1 Add repository route for branches page; API client methods
- [x] 3.2 Build UI: default section + all list, search, last update, ahead/behind
- [x] 3.3 Manage actions: create, delete, set default; hide write controls without manage
- [x] 3.4 Create-PR button prefilling source/target; i18n zh-CN / en-US
- [x] 3.5 Wire browse page branch-count (or equivalent) navigation to branches page

## 4. PR schema and domain

- [x] 4.1 Flyway h2 + mysql: `sys_git_pull_request`, `sys_git_pull_request_comment`
- [x] 4.2 Domain, mapper, XML; repository-scoped PR number allocation

## 5. PR backend core

- [x] 5.1 Create/list/get/close PR services with push/pull/manage matrix (author may close own)
- [x] 5.2 Commits-ahead and files/diff endpoints (reuse commit-detail truncation rules)
- [x] 5.3 Comment list/create (whole-PR; optional parent reply); push to write, pull to read
- [x] 5.4 REST under `/system/git/repo/{id}/pulls/**` + unit tests for permissions and states

## 6. Server-side merge

- [x] 6.1 JGit three-way merge helper on bare repo (merge commit; FF when applicable)
- [x] 6.2 Merge API: manage-only; on success update target ref + mark merged; on conflict leave ref/PR unchanged
- [x] 6.3 Tests: clean merge, conflict rejection, develop denied

## 7. PR UI

- [x] 7.1 Routes: list, new (query prefills), detail
- [x] 7.2 Create form + list filters (at least open/closed/merged or tabs)
- [x] 7.3 Detail: metadata, commits, files/diff, discussion thread, merge/close by permission
- [x] 7.4 i18n zh-CN / en-US

## 8. Verification

- [ ] 8.1 Manual: branches page search / ahead-behind / manage create-delete-default; develop cannot write branches
- [ ] 8.2 Manual: develop opens PR + comments; admin merges; develop cannot merge; conflict path shows error
- [ ] 8.3 Manual: browse → branches → create PR prefills
- [x] 8.4 Run new/updated backend tests
  - `SysGitBrowseServiceImplTest` + `GitPullRequestMergeHelperTest` + `SysGitPullRequestServiceImplTest`：31 tests passed
  - 前端 `build:prod` 已由各 UI agent 通过；浏览器手工验收见 8.1–8.3
