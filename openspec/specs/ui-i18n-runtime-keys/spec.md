## ADDED Requirements

### Requirement: Static i18n keys used in source resolve in zh-CN

Every statically referenced i18n key in `cat2bug-platform-ui/src` (via `$t('...')` or `i18n.t('...')` with a string literal) MUST exist in `i18n-zh-CN.json` with a non-empty value, except keys matching documented dynamic patterns (e.g. `week0`–`week6`, `defect.log.media-*` + `-img`/`-file` suffix).

#### Scenario: API copy failure message

- **WHEN** the user triggers API key copy failure in project API settings
- **THEN** the UI displays a localized message from `project.api.copy-failed`
- **AND** the key exists in zh-CN and all synced locale files

#### Scenario: Plan defect restart count in Markdown template

- **WHEN** the plan Markdown editor inserts defect restart count placeholder
- **THEN** it uses key `plan.defect-restart-count` (or equivalent documented key present in zh-CN)
- **AND** does not reference a non-existent key such as `plan.defect-re-open-count`

### Requirement: Feishu notification UI keys resolve in every UI language

Keys referenced by `feishu.vue` and related Feishu settings views, including `feishu.hook-required`, `feishu.please-enter-hook`, and `feishu.please-enter-mobile`, MUST exist in zh-CN and in every locale file after sync.

#### Scenario: English UI Feishu form validation

- **WHEN** the user leaves the Webhook field empty on the Feishu settings form with UI language `en_US`
- **THEN** the validation message is human-readable English text
- **AND** not the raw key `feishu.hook-required`

### Requirement: Optional check mode reports unresolved static keys

The i18n maintenance tooling SHOULD provide a check mode that exits non-zero when static keys used in `src` are missing from `i18n-zh-CN.json`.

#### Scenario: CI or developer runs key check before merge

- **WHEN** `scripts/sync-i18n-ui.py --check` or documented npm script runs after code changes
- **THEN** it lists any missing static keys and returns a non-zero exit code if any are found
