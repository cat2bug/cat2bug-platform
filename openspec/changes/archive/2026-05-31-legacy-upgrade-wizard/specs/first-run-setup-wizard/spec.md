## MODIFIED Requirements

### Requirement: System detects first-run installation state

The system SHALL expose whether initial setup is complete. The system MUST treat the instance as installed when `cat2bug.install.completed` is `true` in the disk install file at `cat2bug.install.config-path`, or when install skip is enabled via `cat2bug.install.skip` or environment variable `CAT2BUG_INSTALL_SKIP=true`.

The system MUST treat the instance as not installed when the disk install file is absent, or when `cat2bug.install.completed` is absent or not `true`.

The system MUST NOT treat mere presence of the disk install file as installed.

When legacy data exists without a completed disk install file and upgrade is required, the system MUST NOT mark the instance as installed until the upgrade wizard completes successfully.

#### Scenario: Fresh deployment is not installed

- **WHEN** the application starts with no disk install file and install skip is false
- **THEN** `GET /setup/status` returns `installed: false`

#### Scenario: Completed installation is installed

- **WHEN** the disk install file contains `cat2bug.install.completed: true`
- **THEN** `GET /setup/status` returns `installed: true`

#### Scenario: Disk install file without completed flag is not installed

- **WHEN** a disk install file exists but `cat2bug.install.completed` is not `true`
- **THEN** `GET /setup/status` returns `installed: false`

#### Scenario: Legacy data with upgrade pending is not installed

- **WHEN** legacy data exists, no completed disk install file exists, upgrade skip is false, and upgrade is pending
- **THEN** `GET /setup/status` returns `installed: false` and the UI routes to `/upgrade`

#### Scenario: Developer first run uses wizard

- **WHEN** a developer starts the application from a fresh clone without a disk install file and without legacy data
- **THEN** the UI navigates to `/setup` and install file is created only after successful submit

### Requirement: Uninstalled instance routes users to setup

The system SHALL redirect unauthenticated clients to the setup wizard when not installed and upgrade is not required. When upgrade is required, the system MUST redirect to the upgrade wizard instead. API requests outside setup or upgrade endpoints MUST be rejected or redirected except for setup, upgrade, health, and static assets.

#### Scenario: User opens application root before install

- **WHEN** `installed` is `false`, upgrade is not required, and the user navigates to `/`
- **THEN** the UI navigates to `/setup`

#### Scenario: Legacy instance routes to upgrade not setup

- **WHEN** `installed` is `false`, legacy data exists, and `upgradeRequired` is `true`
- **THEN** the UI navigates to `/upgrade`

#### Scenario: Setup API is accessible without login before install

- **WHEN** `installed` is `false`, upgrade is not required, and the client calls `POST /setup/submit` with valid payload
- **THEN** the request is accepted without authentication

#### Scenario: Setup API blocked after install

- **WHEN** `installed` is `true` and the client calls `POST /setup/submit`
- **THEN** the request is rejected with an appropriate error

#### Scenario: Setup API blocked during upgrade pending

- **WHEN** `upgradeRequired` is `true` and the client calls `POST /setup/submit`
- **THEN** the request is rejected with an appropriate error
