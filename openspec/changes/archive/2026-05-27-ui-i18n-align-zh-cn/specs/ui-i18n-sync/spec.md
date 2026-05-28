## ADDED Requirements

### Requirement: UI locale files share the same key set as Simplified Chinese

The system SHALL treat `cat2bug-platform-ui/src/utils/i18n/i18n-zh-CN.json` as the authoritative key set and key order for all other UI locale files (`i18n-en-US.json`, `i18n-zh-TW.json`, `i18n-ar.json`, `i18n-ja-JP.json`, `i18n-ko-KR.json`, `i18n-ru.json`). After synchronization, each target file MUST contain exactly the same keys as zh-CN in the same order.

#### Scenario: English locale has no missing keys after sync

- **WHEN** `scripts/sync-i18n-ui.py` is run against the repository
- **THEN** every key present in `i18n-zh-CN.json` exists in `i18n-en-US.json`
- **AND** no key exists only in `i18n-en-US.json` unless listed in a documented deprecation removal list processed by the script

#### Scenario: Legacy Feishu keys are migrated or duplicated per alias table

- **WHEN** zh-CN defines `feishu.please-enter-hook` and code still references `feishu.hook-required`
- **THEN** synchronization MUST ensure `feishu.hook-required` exists in all locale files with a non-empty value (copied from alias target or explicit override)

### Requirement: UI i18n sync script with fallback chain

The project SHALL provide `scripts/sync-i18n-ui.py` that updates target locale JSON files using: existing translation in target file, then `LOCALE_OVERRIDES` for that file, then `i18n-en-US.json`, then zh-CN value as last resort.

#### Scenario: New key added only to zh-CN

- **WHEN** a developer adds key `project.api.copy-failed` to `i18n-zh-CN.json` only
- **AND** runs the sync script
- **THEN** all six other locale files gain the key with English or Chinese fallback until overridden

### Requirement: Documented frontend i18n maintenance workflow

The repository SHALL document that new or changed UI strings MUST be added to `i18n-zh-CN.json` first, then propagated via `scripts/sync-i18n-ui.py`, consistent with backend `i18n/README.md` for `messages.properties`.

#### Scenario: Contributor follows documented steps

- **WHEN** a contributor reads the frontend or root i18n maintenance note
- **THEN** they find the command to run after editing zh-CN and the list of supported locale files
