## 1. Backend tag service & JGit helpers

- [x] 1.1 Add tag DTO / create request (name, message, targetRef, targetCommitId) and list item fields (annotated, message, commit summary, releaseId)
- [x] 1.2 Extend `GitReleaseTagHelper` (or new `GitTagHelper`) with: list tags (+ peel), create annotated tag, delete tag ref, reuse validateTagName / resolveTargetCommit
- [x] 1.3 Implement service methods: listTags(q), createTag, deleteTag(cascadeRelease) with manage/pull ACL matching branches
- [x] 1.4 On delete with cascade: reuse release asset+DB cleanup then delete tag; without flag and release present → 409
- [x] 1.5 Enqueue existing tag webhook events on successful create/delete via platform API

## 2. Backend REST & tests

- [x] 2.1 Expose `GET/POST /system/git/repo/{repositoryId}/tags` and `DELETE .../tags/{name}?cascadeRelease=`
- [x] 2.2 Add i18n message keys for tag validation, conflict, not found, cascade required
- [x] 2.3 Unit/integration tests: list/filter, create annotated, duplicate rejected, delete alone, delete 409 without cascade, cascade deletes release+tag

## 3. Frontend API & routing

- [x] 3.1 Add `gitRepo.js` API helpers for list/create/delete tags
- [x] 3.2 Register `code/:slug/tags` route (+ legacy numeric redirect if branches has one) and route-guard slug resolution
- [x] 3.3 Map Tags page in `GitRepoShell` to Code tab (`active-tab="tags"`)

## 4. Tags page UI

- [x] 4.1 Implement `tags.vue`: toolbar search, list columns (name, description, commit), empty state
- [x] 4.2 Create `el-dialog`: name, description, target (branch/commit); call create API; refresh list
- [x] 4.3 Row actions: download via existing archive-by-ref; create release (prefill) or open existing release; delete with cascade confirm when `releaseId` set
- [x] 4.4 Add zh-CN / en-US i18n keys; manage-gated create/delete controls
- [x] 4.5 Add browse entry link to Tags (alongside branches entry if present)

## 5. Verification

- [x] 5.1 Manual smoke: create annotated tag → list/search → download → create release from tag → delete tag cascades release; delete release alone leaves tag
- [x] 5.2 Confirm release-created lightweight tags appear with empty description and can be deleted/cascaded correctly
