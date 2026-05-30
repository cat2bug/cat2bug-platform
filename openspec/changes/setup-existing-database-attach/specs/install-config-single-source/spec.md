## MODIFIED Requirements

### Requirement: Legacy deployments auto-generate disk install

When no disk install file exists and upgrade skip is enabled (`cat2bug.upgrade.skip=true` or `CAT2BUG_UPGRADE_SKIP=true`), the system MAY auto-generate `config/install/application-install.yml` from the current Environment and set `cat2bug.install.completed` to `true` without showing setup or upgrade wizards.

When no disk install file exists and upgrade skip is false, the system MUST NOT auto-generate a completed install file solely because legacy data or Flyway history exists. Legacy attach MUST occur through the setup wizard using `databaseMode: existing`.

#### Scenario: Legacy MySQL instance with upgrade skip receives install file

- **WHEN** an instance previously used MySQL profile configuration, has legacy data, upgrade skip is true, and no disk install file exists
- **THEN** startup migration writes a completed install file reflecting effective datasource and cache settings

#### Scenario: Legacy instance without skip uses setup attach

- **WHEN** legacy data exists, no disk install file exists, and upgrade skip is false
- **THEN** the system does not write a completed install file until setup submit with `databaseMode: existing`

#### Scenario: Fresh empty database does not auto-migrate

- **WHEN** the database is empty and no legacy admin exists
- **THEN** the system does not write a completed install file until setup submit

### Requirement: Bootstrap applies in-memory H2 template before install exists

When no disk install file exists and install is not skipped, the system SHALL inject properties from the H2 classpath template into the Environment as a high-precedence property source without writing the file to disk. The bootstrap H2 JDBC URL MUST use the default database name `cat2bug_platform` at `./data/cat2bug_platform` until setup submit writes the chosen name to the install file.

#### Scenario: Bootstrap does not create disk file

- **WHEN** the application starts for the first time without `config/install/application-install.yml`
- **THEN** no `application-install.yml` file is created on disk until setup submit or upgrade-skip legacy migration

#### Scenario: Bootstrap enables setup APIs

- **WHEN** bootstrap mode is active
- **THEN** the application can connect to embedded H2 and serve `/setup/**` APIs
