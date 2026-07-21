## 1. Database and domain

- [x] 1.1 Add Flyway `V1_1_0_7__git_repo_releases.sql` (h2 + mysql) for `sys_git_release` and `sys_git_release_asset`
- [x] 1.2 Add Java domain/mapper/XML for release and asset entities
- [x] 1.3 Define upload path layout under `cat2bug.profile` and size/count limit constants

## 2. Release backend

- [x] 2.1 Implement JGit tag ensure/create/bind helpers on bare repo
- [x] 2.2 Implement release service: create/update/delete with manage permission; list/get with pull + draft filtering
- [x] 2.3 Implement asset upload/list/delete/download with permission rules
- [x] 2.4 Expose REST under `/system/git/repo/{repositoryId}/releases/**`
- [x] 2.5 Unit/integration tests for permission matrix, draft visibility, tag bind conflict

## 3. Contributors backend

- [x] 3.1 Implement commit-author aggregation with scan/return limits and member matching
- [x] 3.2 Expose `GET /system/git/repo/{repositoryId}/contributors`
- [x] 3.3 Tests for aggregation keys (email vs name) and pull denial

## 4. Frontend APIs and browse aside

- [x] 4.1 Add `gitRepo.js` client methods for releases, assets, contributors
- [x] 4.2 Add Releases aside on `browse.vue` (always visible; empty-state + Create for manage)
- [x] 4.3 Add Contributors aside (hide when count is 0; avatars + All)
- [x] 4.4 Enforce aside order: About → Releases → Contributors → Recent activity
- [x] 4.5 Add i18n keys (zh-CN / en-US)

## 5. Release create/list UI

- [x] 5.1 Build create/edit release flow (tag, title, body, pre-release, draft, assets)
- [x] 5.2 Build release detail/list view reachable from aside
- [x] 5.3 Wire asset upload/download/delete; surface limit errors
  - Upload on create; download/delete on detail page

## 6. Contributors list UI

- [x] 6.1 Build full contributors list from All action
- [x] 6.2 Reuse avatar matching consistent with browse commit authors

## 7. Verification

- [x] 7.1 Manual check: empty releases aside + create as admin; pull-only cannot create
- [x] 7.2 Manual check: publish with asset; pull user downloads; draft hidden from pull-only
- [x] 7.3 Manual check: contributors aside/list for multi-author repo; empty repo hides contributors
- [x] 7.4 Run relevant backend tests and frontend lint/unit if added

