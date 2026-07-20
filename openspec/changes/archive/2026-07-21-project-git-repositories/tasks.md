## 1. Foundation

- [x] 1.1 Add JGit dependency to the appropriate Maven module(s) (JVM path; no ProcessBuilder git)
- [x] 1.2 Run `./scripts/new-flyway-migration.sh git_repository` and define `sys_git_repository` (or final table name) in h2 + mysql
- [x] 1.3 Seed menus and perms (`system:git:repo:manage|push|pull`) for roles `project.create-by`, `project.admin`, `project.develop`, `project.tester` per design matrix
- [x] 1.4 Add config for default git root under `cat2bug.profile` (e.g. `cat2bug.git.root`)

## 2. Repository metadata & disk

- [x] 2.1 Implement domain/mapper/service for project-scoped repository CRUD
- [x] 2.2 On create: init bare repo via JGit at default or absolute path; normalize and validate writability
- [x] 2.3 Support slug rename with disk rename/move and clear clone-URL-changed feedback
- [x] 2.4 Support delete (metadata + optional disk wipe) restricted to manage roles
- [x] 2.5 Enforce manage/pull listing rules; deny team-admin/system-admin bypass without project roles
- [x] 2.6 Unit/integration tests for path validation, role checks, and multi-repo per project

## 3. Smart HTTP

- [x] 3.1 Expose Smart HTTP endpoints for info/refs, upload-pack, receive-pack backed by JGit
- [x] 3.2 Wire HTTP Basic authentication to existing platform login credential verification
- [x] 3.3 Authorize upload-pack with pull roles and receive-pack with push roles; reject others
- [x] 3.4 Reject non-fast-forward for non-manage; allow force-push only for manage roles
- [x] 3.5 Confirm no server-side `git` process is spawned (code review / test guard)
- [ ] 3.6 Manual or scripted JVM verification: `git clone` / `git push` against local server with develop and tester accounts; deny non-role member

## 4. Browse API & UI

- [x] 4.1 REST: list branches, tree, blob, commit log for a repository (pull permission)
- [x] 4.2 Frontend: project code-repo list, clone URL display, manage forms for admin/create-by
- [x] 4.3 Frontend: branch/file browse views; hide manage controls for develop/tester
- [x] 4.4 Slug-change UX warning that old clone URLs break
- [x] 4.5 i18n keys (zh-CN / en-US at minimum) for new UI strings

## 5. Security & polish

- [x] 5.1 Spring Security: separate `/git/**` Basic filter from session JWT APIs without breaking existing auth
- [x] 5.2 Absolute-path safeguards (normalization, writability, minimal deny rules) documented in code comments or TESTING.md
- [x] 5.3 Add `openspec/changes/project-git-repositories/TESTING.md` with JVM clone/push/browse and permission matrix checklist
- [x] 5.4 Optional follow-up note: Native Docker smoke is out of MVP; document if deferred
