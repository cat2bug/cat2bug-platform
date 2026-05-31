## ADDED Requirements

### Requirement: Classpath install templates provide full infrastructure skeletons

The system SHALL ship complete install configuration templates on the classpath at `defaults/application-install-h2.yml` and `defaults/application-install-mysql.yml`. Each template MUST include `spring.database-type`, full `spring.datasource` (including Druid pool settings), optional `spring.redis` (MySQL template), optional `spring.h2.console` (H2 template), `cat2bug.cache`, default `cat2bug.profile` / `cat2bug.temp`, and placeholder `cat2bug.ai` / `logging.file` sections as applicable.

#### Scenario: H2 template is loadable at bootstrap

- **WHEN** the application starts without a disk install file and bootstrap mode is active
- **THEN** the H2 classpath template can be loaded and applied to the Environment

#### Scenario: MySQL template used on wizard submit

- **WHEN** setup submit selects database type `mysql`
- **THEN** the written disk install file is based on the MySQL classpath template merged with user connection fields

### Requirement: Disk install file is not shipped in the repository

The repository MUST NOT commit a runnable `config/install/application-install.yml`. The path `config/install/application-install.yml` MUST be listed in `.gitignore`. The repository MAY include `config/install/.gitkeep` as a directory placeholder.

#### Scenario: Fresh clone has no disk install

- **WHEN** a developer clones the repository and has not run setup
- **THEN** `config/install/application-install.yml` does not exist on disk

### Requirement: Bootstrap applies in-memory H2 template before install exists

When no disk install file exists and install is not skipped, the system SHALL inject properties from the H2 classpath template into the Environment as a high-precedence property source without writing the file to disk. The bootstrap H2 JDBC URL MUST use the default database name `cat2bug_platform` at `./data/cat2bug_platform` until setup submit writes the chosen name to the install file.

#### Scenario: Bootstrap does not create disk file

- **WHEN** the application starts for the first time without `config/install/application-install.yml`
- **THEN** no `application-install.yml` file is created on disk until setup submit or upgrade-skip legacy migration

#### Scenario: Bootstrap enables setup APIs

- **WHEN** bootstrap mode is active
- **THEN** the application can connect to embedded H2 and serve `/setup/**` APIs

### Requirement: Setup submit writes complete disk install configuration

On successful setup submit, the system SHALL write the configured install path with the merged full template content (database, cache, storage paths, logging path, AI settings, Redis when applicable) and MUST set `cat2bug.install.completed` to `true` in that file.

#### Scenario: Install file contains Druid pool settings

- **WHEN** setup submit succeeds
- **THEN** the disk install file includes Druid pool configuration not only `druid.master` URL credentials

#### Scenario: Completed flag written to install file

- **WHEN** setup submit succeeds
- **THEN** `cat2bug.install.completed` is `true` in the disk install file

### Requirement: Legacy deployments auto-generate disk install

When no disk install file exists and upgrade skip is enabled (`cat2bug.upgrade.skip=true` or `CAT2BUG_UPGRADE_SKIP=true`), the system MAY auto-generate `config/install/application-install.yml` from the current Environment and set `cat2bug.install.completed` to `true` without showing setup or upgrade wizards.

When no disk install file exists and upgrade skip is false, the system MUST NOT auto-generate a completed install file solely because legacy data or Flyway history exists. Legacy attach MUST occur through the setup wizard using `databaseMode: existing`.

When upgrade skip is disabled and a completed install file exists, the system MUST defer writing `cat2bug.install.completed` until the user completes the upgrade wizard submit successfully when upgrade is required.

#### Scenario: Legacy MySQL instance with upgrade skip receives install file

- **WHEN** an instance previously used MySQL profile configuration, has legacy data, upgrade skip is true, and no disk install file exists
- **THEN** startup migration writes a completed install file reflecting effective datasource and cache settings

#### Scenario: Legacy instance without skip uses setup attach

- **WHEN** legacy data exists, no disk install file exists, and upgrade skip is false
- **THEN** the system does not write a completed install file until setup submit with `databaseMode: existing`

#### Scenario: Fresh empty database does not auto-migrate

- **WHEN** the database is empty and no legacy admin exists
- **THEN** the system does not write a completed install file until setup submit

#### Scenario: Upgrade skip writes install file silently

- **WHEN** legacy data exists, no disk install file exists, and `CAT2BUG_UPGRADE_SKIP=true`
- **THEN** startup migration MAY write an install file with `cat2bug.install.completed: true` without showing the upgrade wizard

### Requirement: Upgrade config merge preserves existing values

When merging configuration for upgrade submit, the system SHALL preserve existing non-empty values from the Environment and partial disk install files over classpath template defaults. Template values MUST be applied only for keys that are missing or empty in the existing configuration, except when the user explicitly overrides a field in the upgrade wizard.

#### Scenario: Custom log path preserved

- **WHEN** the Environment contains a non-empty `logging.file.path` and the template proposes a different default
- **THEN** the merged install file retains the Environment log path

#### Scenario: Template fills missing Redis section

- **WHEN** the user selects Redis cache in the upgrade wizard but no `spring.redis` section exists in existing configuration
- **THEN** the merged install file includes Redis settings from the template merged with wizard input

### Requirement: Database profile YAML files are removed

The system MUST NOT require `application-h2.yml` or `application-mysql.yml` on the classpath for normal operation after this change is fully deployed. `application.yml` MUST NOT set `spring.profiles.active` from `spring.database-type`.

#### Scenario: Application starts with only install file after setup

- **WHEN** `application-install.yml` exists with `completed: true` and profile YAML files are absent
- **THEN** the application starts successfully using imported install configuration
