## MODIFIED Requirements

### Requirement: Legacy deployments auto-generate disk install

When no disk install file exists and the database indicates a legacy installation (admin user with `user_id=1` exists, or Flyway has successful migrations), the system SHALL set upgrade requirement instead of silently writing a completed install file at startup, unless upgrade skip is enabled via `cat2bug.upgrade.skip` or `CAT2BUG_UPGRADE_SKIP=true`.

When upgrade skip is enabled, the system MAY generate `config/install/application-install.yml` from the current Environment, set `cat2bug.install.completed` to `true`, and proceed without the upgrade wizard.

When upgrade skip is disabled, the system MUST defer writing `cat2bug.install.completed` until the user completes the upgrade wizard submit successfully.

#### Scenario: Legacy MySQL instance enters upgrade wizard

- **WHEN** an upgraded instance previously used MySQL profile configuration, has legacy data, no disk install file exists, and upgrade skip is false
- **THEN** startup sets `upgradeRequired` and does not write a completed install file until upgrade submit succeeds

#### Scenario: Fresh empty database does not auto-migrate

- **WHEN** the database is empty and no legacy admin exists
- **THEN** the system does not write a completed install file until setup submit

#### Scenario: Upgrade skip writes install file silently

- **WHEN** legacy data exists, no disk install file exists, and `CAT2BUG_UPGRADE_SKIP=true`
- **THEN** startup migration MAY write an install file with `cat2bug.install.completed: true` without showing the upgrade wizard

## ADDED Requirements

### Requirement: Upgrade config merge preserves existing values

When merging configuration for upgrade submit, the system SHALL preserve existing non-empty values from the Environment and partial disk install files over classpath template defaults. Template values MUST be applied only for keys that are missing or empty in the existing configuration, except when the user explicitly overrides a field in the upgrade wizard.

#### Scenario: Custom log path preserved

- **WHEN** the Environment contains a non-empty `logging.file.path` and the template proposes a different default
- **THEN** the merged install file retains the Environment log path

#### Scenario: Template fills missing Redis section

- **WHEN** the user selects Redis cache in the upgrade wizard but no `spring.redis` section exists in existing configuration
- **THEN** the merged install file includes Redis settings from the template merged with wizard input
