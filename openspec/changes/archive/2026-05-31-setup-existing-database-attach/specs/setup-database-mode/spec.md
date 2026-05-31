## ADDED Requirements

### Requirement: Setup exposes configurable database name for H2 and MySQL

The setup wizard database step SHALL require a database name for both H2 and MySQL. The default name MUST be `cat2bug_platform`. The name MUST match `[A-Za-z0-9_]+`.

For H2, the effective file path MUST be `./data/{databaseName}.mv.db`.

For MySQL, the effective schema name MUST be the configured database name on the connection form.

#### Scenario: H2 default database name

- **WHEN** the user opens the setup database step without changing defaults
- **THEN** the H2 database name is `cat2bug_platform` and the expected file is `./data/cat2bug_platform.mv.db`

#### Scenario: Invalid database name rejected

- **WHEN** the user submits a database name containing characters outside `[A-Za-z0-9_]+`
- **THEN** validation fails before test or submit

### Requirement: Database test API returns new or existing mode

The system SHALL expose database connectivity testing that returns `databaseMode` with value `new` or `existing`.

Existence MUST be determined only by whether the database artifact exists, not by business tables:

- H2: `./data/{databaseName}.mv.db` exists → `existing`; otherwise `new`
- MySQL: schema exists in `information_schema.SCHEMATA` → `existing`; otherwise `new`

#### Scenario: H2 new database mode

- **WHEN** `./data/cat2bug_platform.mv.db` does not exist and the user tests H2 connection with name `cat2bug_platform`
- **THEN** the test succeeds and returns `databaseMode: new`

#### Scenario: H2 existing database mode

- **WHEN** `./data/cat2bug_platform.mv.db` exists and the user tests H2 connection with name `cat2bug_platform`
- **THEN** the test succeeds and returns `databaseMode: existing`

#### Scenario: MySQL new database mode

- **WHEN** the MySQL schema does not exist and connection credentials are valid
- **THEN** the test succeeds and returns `databaseMode: new`

#### Scenario: MySQL existing database mode

- **WHEN** the MySQL schema already exists and connection credentials are valid
- **THEN** the test succeeds and returns `databaseMode: existing`

#### Scenario: Existing MySQL schema does not fail test

- **WHEN** the MySQL schema already exists
- **THEN** the database test MUST NOT fail solely because the schema exists

### Requirement: Setup submit respects database mode for schema initialization

Setup submit MUST accept `databaseMode` and apply conditional schema actions:

- `new`: H2 runs Flyway migrate; MySQL creates the schema if missing then runs Flyway migrate
- `existing`: the system MUST NOT run Flyway migrate and MUST NOT create the MySQL schema

In both modes, the system MUST write the completed install file and set `cat2bug.install.completed` to `true`.

#### Scenario: New H2 database migrates on submit

- **WHEN** setup submit uses H2 with `databaseMode: new`
- **THEN** Flyway migrations run against the configured H2 file database

#### Scenario: Existing H2 database skips migrate on submit

- **WHEN** setup submit uses H2 with `databaseMode: existing`
- **THEN** Flyway migrate is not executed during setup submit

#### Scenario: New MySQL database creates schema and migrates

- **WHEN** setup submit uses MySQL with `databaseMode: new`
- **THEN** the schema is created if missing and Flyway migrate runs

#### Scenario: Existing MySQL database skips migrate on submit

- **WHEN** setup submit uses MySQL with `databaseMode: existing`
- **THEN** the system does not create the schema and does not run Flyway migrate during setup submit

#### Scenario: Submit re-validates database mode

- **WHEN** setup submit includes `databaseMode` that does not match the current existence probe
- **THEN** submit fails with a validation error

### Requirement: Existing database attach overwrites admin credentials

When setup submit uses `databaseMode: existing`, the system SHALL create or update the admin user (`user_id=1`) using the username and password from the setup wizard, overwriting existing credentials.

#### Scenario: Existing legacy database admin overwritten

- **WHEN** setup submit attaches an existing database with admin username `admin` and password `newpass`
- **THEN** the user with `user_id=1` can log in with username `admin` and password `newpass`
