## 1. Assets

- [x] 1.1 Copy preview 定稿 PNGs into `cat2bug-platform-ui/src/assets/images/error/`
- [x] 1.2 Stop referencing old 404/401 cloud assets and `lock_flag` on error pages

## 2. Shell component

- [x] 2.1 Add `ErrorStateShell` (illustration / code / title / description / actions slot)
- [x] 2.2 Wire light/dark art from `themeMode`

## 3. Pages & i18n

- [x] 3.1 Rewrite `404.vue` / `401.vue` to use the shell
- [x] 3.2 Rewrite `team-lock.vue` / `project-lock.vue` to use the shell (keep business actions)
- [x] 3.3 Add 404/401 i18n keys (at least zh-CN + en-US; mirror other locales that already have error.*)

## 4. Docs

- [x] 4.1 Update `docs/04-UI规范/组件与交互约定.md` with error-page vs empty-state distinction
