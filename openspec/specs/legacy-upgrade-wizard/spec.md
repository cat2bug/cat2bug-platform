## ADDED Requirements

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

### Requirement: Upgrade mode fully locks business access

While `upgradeRequired` is `true` and `state` is `pending`, `running`, `failed`, or `restart_required`, the system SHALL reject all business API requests including login and authenticated endpoints. The system MUST allow only `/upgrade/**` APIs, `/upgrade/status`, `/version`, and static assets required to render the upgrade wizard.

#### Scenario: Login blocked during upgrade pending

- **WHEN** `upgradeRequired` is `true` and `state` is `pending`
- **THEN** `POST /login` is rejected with an appropriate error message

#### Scenario: Upgrade API accessible without authentication

- **WHEN** `upgradeRequired` is `true` and the client calls `GET /upgrade/status`
- **THEN** the request succeeds without authentication

#### Scenario: Business API blocked during upgrade failed

- **WHEN** `state` is `failed` after a migration error
- **THEN** business API requests are rejected and the client remains on the upgrade wizard

#### Scenario: Normal access after upgrade completed and restarted

- **WHEN** `state` is `completed` after restart
- **THEN** business APIs and login are available

### Requirement: Upgrade wizard routes users to upgrade flow

The system SHALL redirect clients to `/upgrade` when `upgradeRequired` is `true` and upgrade is not completed. The upgrade route MUST take precedence over normal application routes when upgrade is active.

#### Scenario: User opening root during upgrade

- **WHEN** `upgradeRequired` is `true` and the user navigates to `/`
- **THEN** the UI navigates to `/upgrade`

### Requirement: Upgrade wizard collects configuration confirmation

The upgrade wizard SHALL present an overview step (source version, target version, pending migrations, backup reminder), a configuration confirmation step showing diffs with preserve-first defaults, a preflight step (database connectivity, optional Redis, path writability), and an execution step.

The configuration merge MUST preserve existing non-empty values from the Environment and any partial disk install over classpath template defaults unless the user explicitly overrides a field in the wizard.

#### Scenario: Existing datasource URL preserved by default

- **WHEN** the upgrade wizard loads configuration for a legacy MySQL instance with a custom JDBC URL in the Environment
- **THEN** the suggested install file retains the existing JDBC URL unless the user changes it

#### Scenario: Missing cache type receives template default

- **WHEN** `cat2bug.cache.type` is absent from existing configuration
- **THEN** the diff highlights a suggested default from the classpath template

#### Scenario: User override replaces preserved value

- **WHEN** the user explicitly changes the Redis host in the upgrade wizard and submits
- **THEN** the written install file contains the user-provided Redis host

### Requirement: Upgrade submit executes config write and migration idempotently

On `POST /upgrade/submit` or `POST /upgrade/retry`, the system SHALL execute upgrade steps in order: configuration merge and disk write, then Flyway migrate, then mark `cat2bug.install.completed` to `true`. The system MUST record `lastStep` and MUST skip steps already completed successfully in a prior attempt when retrying from `failed` state.

On full success, the system MUST set `state` to `restart_required` and instruct the user to restart the application.

#### Scenario: Successful first-time upgrade submit

- **WHEN** the user submits a valid upgrade payload from `pending` state
- **THEN** the disk install file is written, Flyway migrations run, `cat2bug.install.completed` is `true`, and `state` becomes `restart_required`

#### Scenario: Migration failure enables retry

- **WHEN** Flyway migrate fails during upgrade submit
- **THEN** `state` is `failed`, `lastStep` is `migration`, `lastError` contains a summary, and the UI offers retry

#### Scenario: Retry skips completed config step

- **WHEN** config write succeeded but migration failed, and the user calls retry
- **THEN** the system does not rewrite config unless the user changed wizard fields, and re-attempts migration only

#### Scenario: Restart clears restart_required

- **WHEN** the application restarts after a successful upgrade with `restart_required`
- **THEN** `state` becomes `completed`, `upgradeRequired` is `false`, and normal operation is available

### Requirement: Upgrade documents single-instance constraint

The upgrade wizard SHALL display a notice that only one Cat2Bug process must run during upgrade. The system MUST NOT implement multi-instance upgrade coordination in this capability.

#### Scenario: Overview shows single-instance notice

- **WHEN** the user opens the upgrade overview step
- **THEN** the UI displays guidance to stop other instances before proceeding
