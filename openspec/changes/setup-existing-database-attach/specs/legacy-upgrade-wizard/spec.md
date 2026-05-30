## MODIFIED Requirements

### Requirement: System detects upgrade requirement

The system SHALL expose whether a version upgrade is required via `GET /upgrade/status`. The response MUST include `upgradeRequired` (boolean), `state` (`pending`, `running`, `failed`, `restart_required`, `completed`), `targetVersion`, `pendingMigrations` (when applicable), `lastError`, and `lastStep`.

The system MUST set `upgradeRequired` to `true` when upgrade skip is false and any of the following holds:

- A completed disk install file exists (`cat2bug.install.completed=true`) and Flyway detects pending migrations for the active database type
- Recorded `cat2bug.upgrade.completedVersion` is lower than the application target version when version tracking is enabled
- An in-progress upgrade state machine is active (`pending`, `running`, `failed`, or `restart_required`)

The system MUST NOT set `upgradeRequired` to `true` solely because legacy data exists when no completed install file exists.

The system MUST NOT set `upgradeRequired` when `cat2bug.upgrade.skip` is `true` or environment variable `CAT2BUG_UPGRADE_SKIP=true`.

#### Scenario: Installed instance with pending migrations requires upgrade

- **WHEN** `cat2bug.install.completed=true` and Flyway reports pending migrations and upgrade skip is false
- **THEN** `GET /upgrade/status` returns `upgradeRequired: true` and `state: pending`

#### Scenario: Legacy data without install file does not require upgrade wizard

- **WHEN** legacy data exists, no completed disk install file exists, and upgrade skip is false
- **THEN** `GET /upgrade/status` returns `upgradeRequired: false`

#### Scenario: Fresh empty database does not require upgrade

- **WHEN** the database is empty and no legacy admin exists
- **THEN** `GET /upgrade/status` returns `upgradeRequired: false`

#### Scenario: Upgrade skip allows silent migration after install

- **WHEN** a completed install exists, upgrade skip is true, and Flyway reports pending migrations
- **THEN** the system runs Flyway migrate during startup without showing the upgrade wizard

#### Scenario: Silent migration failure exits process

- **WHEN** upgrade skip is true, install is completed, and silent Flyway migrate fails
- **THEN** the application process exits with a non-zero status code

#### Scenario: Pending Flyway scripts trigger upgrade when not skipped

- **WHEN** a completed install exists but Flyway reports pending migrations and upgrade skip is false
- **THEN** `GET /upgrade/status` returns `upgradeRequired: true`
