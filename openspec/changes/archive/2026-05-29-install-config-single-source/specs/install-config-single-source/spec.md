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

When no disk install file exists and install is not skipped, the system SHALL inject properties from the H2 classpath template into the Environment as a high-precedence property source without writing the file to disk. The system MUST set `cat2bug.install.bootstrap-mode` to `true` during this phase.

#### Scenario: Bootstrap does not create disk file

- **WHEN** the application starts for the first time without `config/install/application-install.yml`
- **THEN** no `application-install.yml` file is created on disk until setup submit or legacy migration

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

When no disk install file exists and the database indicates a legacy installation (admin user with `user_id=1` exists, or Flyway has successful migrations), the system SHALL generate `config/install/application-install.yml` from the current Environment, set `cat2bug.install.completed` to `true`, and MUST NOT show the setup wizard on subsequent requests in the same run after migration.

#### Scenario: Legacy MySQL instance receives install file

- **WHEN** an upgraded instance previously used MySQL profile configuration and has legacy data
- **THEN** startup migration writes an install file reflecting effective datasource and cache settings

#### Scenario: Fresh empty database does not auto-migrate

- **WHEN** the database is empty and no legacy admin exists
- **THEN** the system does not write a completed install file until setup submit

### Requirement: Database profile YAML files are removed

The system MUST NOT require `application-h2.yml` or `application-mysql.yml` on the classpath for normal operation after this change is fully deployed. `application.yml` MUST NOT set `spring.profiles.active` from `spring.database-type`.

#### Scenario: Application starts with only install file after setup

- **WHEN** `application-install.yml` exists with `completed: true` and profile YAML files are absent
- **THEN** the application starts successfully using imported install configuration
