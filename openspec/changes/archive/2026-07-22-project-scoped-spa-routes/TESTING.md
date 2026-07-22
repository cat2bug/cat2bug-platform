# TESTING — project-scoped-spa-routes

Manual + automated checklist for History routing and `/{teamKey}/{projectKey}/{module}` paths.

## Preflight

- Frontend built with History mode (`createWebHistory`), not hash
- Backend `EmbeddedSpaFallbackFilter` (or nginx `try_files`) serves `index.html` for unknown HTML GETs
- Namespaced Git Smart HTTP `/{t}/{p}/code/{slug}.git/**` is excluded from SPA fallback
- Login account is a member of at least two projects with distinct path keys

Replace `{t}` / `{p}` / `{slug}` with real keys below.

---

## Automated (ran / runnable without full UI)

| Check | How | Status notes |
|-------|-----|--------------|
| Project-scoped route utils | `cd cat2bug-platform-ui && npm run test:unit -- --testPathPattern='project-scoped\|git-repo\|legacy-project-module' --no-coverage` | Unit suites for routes, guards, git-repo paths, legacy redirects |
| SPA vs `.git` path exclusion | `mvn -pl cat2bug-platform-framework test -Dtest=EmbeddedSpaFallbackFilterTest -Dsurefire.failIfNoSpecifiedTests=false` | Asserts `.git` excluded; SPA `code/{slug}` / `defect` not excluded |
| Native SPA smoke (needs live host) | `./deploy/test/native-spa-smoke.sh [http://127.0.0.1:2020]` | `/`, static JS, `/login` SPA fallback, History deep-link `/{t}/{p}/defect` SPA fallback |
| Live `git clone` | Manual (below) | Smoke script does **not** assert Smart HTTP; filter unit tests cover path exclusion |

---

## 6.1 Manual — share / refresh / switch / Git / business pages

### Share URL without `#`

- [ ] Copy project share / open from project list → address is `http(s)://{host}/{t}/{p}` (or `.../dashboard`), **no** `/#/`
- [ ] Share a business deep link (e.g. defect list) → `http(s)://{host}/{t}/{p}/defect` without `#`

### Refresh History deep link

- [ ] While logged in, open `/{t}/{p}/defect` (or another module), hard-refresh → SPA shell loads, module view restores (not bare 404 / JSON)
- [ ] Hard-refresh `/{t}/{p}/code/{slug}` (browse) → SPA restores; not confused with `.git`
- [ ] Hard-refresh `/login` → login page (SPA fallback), not 404

### Project switch changes prefix

- [ ] On `/{t1}/{p1}/defect`, switch to project `{t2}/{p2}` → URL becomes `/{t2}/{p2}/defect` (prefix **must** change; module suffix kept when possible)
- [ ] Sidebar `activeMenu` highlights the namespaced path for the new project

### `.git` clone vs SPA code path

- [ ] Browser: `/{t}/{p}/code/{slug}` → source browse UI (SPA)
- [ ] Clone URL remains `http(s)://{apiHost}/{t}/{p}/code/{slug}.git` (note trailing `.git`)
- [ ] `git clone` / `git ls-remote` against that URL uses Smart HTTP (pack / refs), **not** HTML `index.html`
- [ ] Optional negative: `curl -H 'Accept: text/html' .../code/{slug}.git/info/refs` must not be treated as the primary SPA document (401/403/Git response OK; SPA shell is a fail)

### Business pages under namespace

- [ ] Whitelist modules resolve under prefix: `dashboard`, `defect`, `case`, `plan`, `report`, `document`, `module`, `notice`, `member`, `option`, `api`, `tools`, `code`
- [ ] Illegal third segment → 404 (not another project's page)
- [ ] Legacy roots (e.g. `/defect`, `/project/git-repo/browse/:id/...`) redirect to namespaced paths when current project keys are known

---

## 6.2 Manual — login / setup / upgrade / system unaffected

- [ ] `/login` stays at root (not under `/{t}/{p}/...`)
- [ ] `/setup` and `/upgrade` still reachable without project prefix; install/upgrade flows unchanged
- [ ] System menus (`/system/...`, team admin, etc.) keep original prefixes — not rewritten into project namespace
- [ ] After login, navigating system pages does not force a `/{t}/{p}` prefix onto those routes

---

## Deploy notes

- Embedded: `EmbeddedSpaFallbackFilter` + static under admin resources
- Reverse proxy: `deploy/docker/nginx.conf` uses `try_files ... /index.html` for History; do not put SPA `try_files` on `/{t}/{p}/code/{slug}.git/**` when Git shares the vhost

## Related

- Spec: `specs/spa-history-routing/spec.md`, `specs/project-scoped-module-routes/spec.md`
- Prior path-keys checklist (History URLs): `../archive/2026-07-21-team-project-path-keys/TESTING.md`
