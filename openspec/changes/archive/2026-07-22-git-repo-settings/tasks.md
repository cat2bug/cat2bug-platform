## 1. Routes and shell tab

- [x] 1.1 Add `gitRepoSettingsRoute` / `gitRepoSettingsGeneralRoute` / `gitRepoSettingsDangerRoute` (and path builders) in `git-repo-routes.js`; include `settings` in shell tab resolution helpers
- [x] 1.2 Register Vue routes for `.../code/:slug/settings`, `settings/general`, `settings/danger` (settings root redirects to general); wire `git-repo-route-guards` so settings needs `repositoryId`
- [x] 1.3 Extend `GitRepoShell` with Settings（管理） tab (`shellTab=settings`), gated by manage permission; navigate via settings route helpers

## 2. Settings UI

- [x] 2.1 Add settings layout view (sidebar + `<router-view>`): General + Delete enabled; Members disabled with upcoming hint
- [x] 2.2 Implement general page: form fields aligned with list edit dialog; save via existing update API; on slug change `replace` to new slug settings URL
- [x] 2.3 Implement danger/delete page: confirm then existing delete API; success → project repository list
- [x] 2.4 Add i18n keys (zh-CN / en-US) for tab, sidebar, forms, placeholder, errors

## 3. Entry points and cleanup

- [x] 3.1 Change project repository list Settings action to navigate to `settings/general`; remove or disable primary list edit dialog
- [x] 3.2 Align any other settings shortcuts (featured card, etc.) to the same shell settings entry
- [x] 3.3 Guard non-manage deep links to settings (redirect/deny)

## 4. Verification

- [x] 4.1 Unit tests for route helpers / tab resolution including `settings`
- [x] 4.2 Manual check: manage sees tab and can edit/delete; non-manage no tab and deep link blocked; members sidebar greyed; list Settings lands on general
