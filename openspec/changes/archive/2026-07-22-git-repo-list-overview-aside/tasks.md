## 1. Backend overview aggregates

- [x] 1.1 Add DTO for project git overview (`repoCount`, `openPrCount`, `collectCount`, `languages[]`)
- [x] 1.2 Implement SQL/mapper: count visible repos, open PRs on those repos, collect rows on those repos for a project + caller visibility rules
- [x] 1.3 Add service method + `GET /system/git/repo/project/{projectId}/overview` with same auth gate as repo list
- [x] 1.4 Unit/integration tests for empty project, multi-user collect sum, open PR filter, unauthorized denial

## 2. Backend light language scan

- [x] 2.1 Add extension→language map and skip-path rules (`node_modules`, `vendor`, `dist`, `build`, `.git`)
- [x] 2.2 Implement per-repo default-branch path walk with per-repo and project file caps; merge counts; Top N + other; percent leveling
- [x] 2.3 Wire language scan into overview service; single-repo failure must not fail whole overview
- [x] 2.4 Unit tests for mapping, caps, skip paths, other bucket, and partial failure

## 3. Frontend API and aside UI

- [x] 3.1 Add `getGitRepoProjectOverview(projectId)` in `gitRepo.js`
- [x] 3.2 Build aside component (or section in `index.vue`): three stats, language bars, members preview via `listMemberOfProject`
- [x] 3.3 Dual-column layout on list page; hide aside below 1200px
- [x] 3.4 On collect toggle success, adjust aside `collectCount` by ±1 (or refresh overview stats)
- [x] 3.5 i18n keys (zh-CN / en-US at minimum; align other locales if present): 仓库、PR、关注、仓库语言、其他、成员

## 4. Verification

- [x] 4.1 Manual check: wide vs narrow viewport; stats match DB; language order plausible; members match project
- [x] 4.2 Run backend tests for overview/language and frontend unit tests if added
