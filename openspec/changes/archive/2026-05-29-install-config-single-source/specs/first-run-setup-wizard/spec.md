## MODIFIED Requirements

### Requirement: System detects first-run installation state

The system SHALL expose whether initial setup is complete. The system MUST treat the instance as installed when `cat2bug.install.completed` is `true` in the disk install file at `cat2bug.install.config-path`, or when install skip is enabled via `cat2bug.install.skip` or environment variable `CAT2BUG_INSTALL_SKIP=true`.

The system MUST treat the instance as not installed when the disk install file is absent, or when `cat2bug.install.completed` is absent or not `true`.

The system MUST NOT treat mere presence of the disk install file as installed.

#### Scenario: Fresh deployment is not installed

- **WHEN** the application starts with no disk install file and install skip is false
- **THEN** `GET /setup/status` returns `installed: false`

#### Scenario: Completed installation is installed

- **WHEN** the disk install file contains `cat2bug.install.completed: true`
- **THEN** `GET /setup/status` returns `installed: true`

#### Scenario: Disk install file without completed flag is not installed

- **WHEN** a disk install file exists but `cat2bug.install.completed` is not `true`
- **THEN** `GET /setup/status` returns `installed: false`

#### Scenario: Legacy migration marks installed without wizard

- **WHEN** legacy data exists, no disk install file exists, and startup migration generates install with `completed: true`
- **THEN** `GET /setup/status` returns `installed: true`

#### Scenario: Developer first run uses wizard

- **WHEN** a developer starts the application from a fresh clone without a disk install file
- **THEN** the UI navigates to `/setup` and install file is created only after successful submit

### Requirement: Setup creates administrator and persists configuration

On successful submit, the system SHALL write `config/install/application-install.yml` (or configured path) with **complete** infrastructure settings merged from classpath templates and wizard input, run schema initialization via Flyway when needed, create the administrator account with the user-provided or default credentials, write `sys.account.registerUser` and `sys.account.captchaEnabled`, set `cat2bug.install.completed` to `true` in the disk install file, and write `cat2bug.install.completed` to `sys_config` for audit. The system MUST NOT reject password `cat2bug`.

#### Scenario: Administrator account created on install

- **WHEN** setup submit succeeds with username `admin` and password `cat2bug`
- **THEN** login with `admin`/`cat2bug` succeeds

#### Scenario: Custom administrator credentials

- **WHEN** setup submit succeeds with a custom username and password
- **THEN** login with those credentials succeeds

#### Scenario: Install requires restart for infrastructure

- **WHEN** setup submit persists database type or paths requiring restart
- **THEN** the response instructs the user to restart the application before normal use

#### Scenario: Init password not in wizard

- **WHEN** the user completes setup
- **THEN** the wizard has not collected or changed `sys.member.initPassword`

#### Scenario: Install file is self-contained after submit

- **WHEN** setup submit succeeds with MySQL and local cache
- **THEN** the disk install file contains datasource, `spring.database-type`, and `cat2bug.cache.type` without requiring `application-mysql.yml`

### Requirement: Docker may skip setup when preconfigured

When environment variable `CAT2BUG_INSTALL_SKIP` is `true`, or `cat2bug.install.skip` is `true`, the system SHALL skip forcing the setup wizard. Preconfiguration MUST be supplied via a disk install file with `cat2bug.install.completed: true` and/or environment property overrides for datasource and related settings.

#### Scenario: Docker compose with skip flag and install file

- **WHEN** the container starts with `CAT2BUG_INSTALL_SKIP=true` and a mounted install file with `completed: true`
- **THEN** the application does not redirect to `/setup`

#### Scenario: Skip without completed install still allows bootstrap

- **WHEN** `CAT2BUG_INSTALL_SKIP=true` and no disk install file exists
- **THEN** the application does not force `/setup` (existing skip semantics preserved)

## REMOVED Requirements

### Requirement: Legacy existing deployment auto-marked

**Reason**: Legacy instances are handled by automatic generation of `application-install.yml` with `completed: true`, not by inferring installed state from `admin` username alone without an install file.

**Migration**: On first startup after upgrade, `InstallConfigMigrationRunner` writes the disk install file from the Environment when legacy schema or admin user is detected.
