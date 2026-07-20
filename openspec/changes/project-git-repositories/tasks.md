## 1. Foundation

- [ ] 1.1 Add JGit dependency to the appropriate Maven module(s) (JVM path; no ProcessBuilder git)
- [ ] 1.2 Run `./scripts/new-flyway-migration.sh git_repository` and define `sys_git_repository` (or final table name) in h2 + mysql
- [ ] 1.3 Seed menus and perms (`system:git:repo:manage|push|pull`) for roles `project.create-by`, `project.admin`, `project.develop`, `project.tester` per design matrix
- [ ] 1.4 Add config for default git root under `cat2bug.profile` (e.g. `cat2bug.git.root`)

## 2. Repository metadata & disk

- [ ] 2.1 Implement domain/mapper/service for project-scoped repository CRUD
- [ ] 2.2 On create: init bare repo via JGit at default or absolute path; normalize and validate writability
- [ ] 2.3 Support slug rename with disk rename/move and clear clone-URL-changed feedback
- [ ] 2.4 Support delete (metadata + optional disk wipe) restricted to manage roles
- [ ] 2.5 Enforce manage/pull listing rules; deny team-admin/system-admin bypass without project roles
- [ ] 2.6 Unit/integration tests for path validation, role checks, and multi-repo per project

## 3. Smart HTTP

- [ ] 3.1 Expose Smart HTTP endpoints for info/refs, upload-pack, receive-pack backed by JGit
- [ ] 3.2 Wire HTTP Basic authentication to existing platform login credential verification
- [ ] 3.3 Authorize upload-pack with pull roles and receive-pack with push roles; reject others
- [ ] 3.4 Reject non-fast-forward for non-manage; allow force-push only for manage roles
- [ ] 3.5 Confirm no server-side `git` process is spawned (code review / test guard)
- [ ] 3.6 Manual or scripted JVM verification: `git clone` / `git push` against local server with develop and tester accounts; deny non-role member

## 4. Browse API & UI

- [ ] 4.1 REST: list branches, tree, blob, commit log for a repository (pull permission)
- [ ] 4.2 Frontend: project code-repo list, clone URL display, manage forms for admin/create-by
- [ ] 4.3 Frontend: branch/file browse views; hide manage controls for develop/tester
- [ ] 4.4 Slug-change UX warning that old clone URLs break
- [ ] 4.5 i18n keys (zh-CN / en-US at minimum) for new UI strings

## 5. Security & polish

- [ ] 5.1 Spring Security: separate `/git/**` Basic filter from session JWT APIs without breaking existing auth
- [ ] 5.2 Absolute-path safeguards (normalization, writability, minimal deny rules) documented in code comments or TESTING.md
- [ ] 5.3 Add `openspec/changes/project-git-repositories/TESTING.md` with JVM clone/push/browse and permission matrix checklist
- [ ] 5.4 Optional follow-up note: Native Docker smoke is out of MVP; document if deferred
