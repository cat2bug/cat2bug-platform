## 1. Backend — JGit clone & import core

- [x] 1.1 Extend `JGitBareRepositoryHelper` (or sibling) with bare clone/mirror from HTTPS URL using request-scoped `CredentialsProvider`
- [x] 1.2 Implement import orchestration: validate slug uniqueness → clone to disk path → insert metadata only on success → wipe disk on failure
- [x] 1.3 Add configurable import timeout (`cat2bug.git.import-timeout-seconds`, default ~120) and map timeout/auth/network errors to user-facing messages
- [x] 1.4 Ensure tokens/passwords never appear in logs or persisted entities

## 2. Backend — APIs

- [x] 2.1 Add `POST /system/git/repo/import` (manage permission) accepting common repo fields + `sourceUrl` + optional `username`/`token`
- [x] 2.2 Add remote list endpoint(s) for providers `github` | `gitlab` | `gitee` with PAT (+ GitLab `host`, default `https://gitlab.com`)
- [x] 2.3 Restrict remote URL schemes to `https` (and optionally `http` with clear policy); reject empty/malformed URLs
- [x] 2.4 Unit/integration tests: public URL import happy path, auth failure cleanup, slug conflict, GitLab custom host list mock

## 3. Frontend — unified dialog

- [x] 3.1 Extract shared `GitRepoCreateDialog` with modes: empty / url / github / gitlab / gitee
- [x] 3.2 Wire list page「新建」and `GitRepoSelect`「+」to the same dialog; remove divergent create-only UI
- [x] 3.3 Mode-specific panels: URL fields; provider PAT + load list + select; GitLab host field
- [x] 3.4 On mode switch: clear token and remote selection; retain shared name/slug when appropriate; prefill from selected remote when fields empty/auto
- [x] 3.5 Submit: empty → existing create API; import modes → import API; loading/error/success; navigate to new repo on success
- [x] 3.6 i18n zh-CN / en-US for all new strings

## 4. Verification

- [x] 4.1 Manual: empty create still works from list and switcher
- [x] 4.2 Manual: import public URL; private URL with PAT; failed import leaves no list row
- [x] 4.3 Manual: GitHub / GitLab (gitlab.com + fake host field) / Gitee list-with-PAT then import
- [x] 4.4 Confirm no token in DB rows, install config, or application logs for a sample import
