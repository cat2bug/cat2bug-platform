## 1. Schema and domain

- [x] 1.1 Run `./scripts/new-flyway-migration.sh git_repo_members` and define `sys_git_repository_member` (h2 + mysql): repository_id, user_id, role, unique (repository_id, user_id)
- [x] 1.2 Add entity / mapper / service for ACL CRUD with last-admin protection
- [x] 1.3 On create and import success paths, insert creator `admin` row in the same success transaction
- [x] 1.4 Backfill: ensure every existing repository has at least one `admin` (prefer `create_by`)

## 2. Effective permission

- [x] 2.1 Change `GitRepositoryAccessService` to `canPull/canPush/canManage(userId, repositoryId)` with ACL-override then project-role fallback
- [x] 2.2 Update all callers (Smart HTTP filter, browse, PR, release, repo controller, tests) to pass `repositoryId`
- [x] 2.3 Unit tests for ACL override, demotion, non-project member write, last-admin reject

## 3. Members API

- [x] 3.1 Implement list / add / update / delete member endpoints under `/system/git/repo/{id}/members` gated by effective manage
- [x] 3.2 Add `GET .../my-access` (or equivalent) returning current user’s effective role for UI
- [x] 3.3 API tests for invite non-project user, duplicate reject, last-admin delete/demote reject

## 4. Frontend settings members

- [x] 4.1 Register `settings/members` route; enable sidebar Members item; remove coming-soon placeholder
- [x] 4.2 Build members page: list, add user + role, change role, remove; wire APIs + i18n
- [x] 4.3 Drive Settings shell tab / settings guards with effective manage (`my-access`), not only `v-hasPermi`
- [ ] 4.4 Manual check: demoted project admin blocked; ACL admin can manage; invite outsider; last admin protected
