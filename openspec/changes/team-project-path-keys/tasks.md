## 1. Data model & migration

- [x] 1.1 Run `./scripts/new-flyway-migration.sh team_project_path_keys` and add `team_key` / `project_key` columns + unique indexes (h2 + mysql)
- [x] 1.2 Backfill strategy for existing rows (generate unique keys; document collision handling)
- [x] 1.3 Domain/mapper/API: expose keys on team and project create/update with validation
- [x] 1.4 Reserved-word denylist constant shared by backend (and mirrored in frontend validation)

## 2. Team / project UX

- [x] 2.1 Team create/edit forms: required `teamKey` field + uniqueness/format errors
- [x] 2.2 Project create/edit forms: required `projectKey` field + team-scoped uniqueness
- [x] 2.3 Rename confirmation warning for key changes (share link + clone URL break)
- [x] 2.4 i18n (zh-CN / en-US) for new labels and errors

## 3. Share link → dashboard

- [x] 3.1 Backend resolve API: `teamKey`+`projectKey` → project (404/403 as designed)
- [x] 3.2 Frontend route `/:teamKey/:projectKey`: auth, set current project, open dashboard
- [x] 3.3 Ensure reserved first-segments do not hit the dynamic route
- [x] 3.4 Show copyable project path `/{teamKey}/{projectKey}` in an appropriate UI surface

## 4. Namespaced Git URLs

- [x] 4.1 Update `getCloneUrl()` / UI to `/{teamKey}/{projectKey}/code/{slug}.git`
- [x] 4.2 Add Smart HTTP controller + authz filter for namespaced path (reuse JGit + role matrix)
- [x] 4.3 Keep `/git/p/{projectId}/{slug}.git` as compatibility alias during the window
- [x] 4.4 Tests: namespaced clone path resolution; legacy path still works when enabled

## 5. Verification

- [x] 5.1 Add `openspec/changes/team-project-path-keys/TESTING.md` (create keys, share link → dashboard, clone new URL, rename warning)
- [x] 5.2 Run relevant unit/integration tests (`mvn -pl … test`) for touched modules
