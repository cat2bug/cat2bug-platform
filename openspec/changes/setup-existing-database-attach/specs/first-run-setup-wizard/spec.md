## MODIFIED Requirements

### Requirement: Setup wizard is shown when installation is not completed

The system SHALL redirect unauthenticated and authenticated users to `/setup` when `cat2bug.install.completed` is not `true` and install skip is not enabled.

When no completed disk install file exists, the system MUST route users to `/setup` regardless of legacy data presence in the database. The system MUST NOT redirect such users to `/upgrade` solely because legacy admin or Flyway history exists.

#### Scenario: Fresh empty database shows setup

- **WHEN** the database is empty and no install file exists
- **THEN** the user is redirected to `/setup`

#### Scenario: Legacy database without install file shows setup

- **WHEN** legacy data exists, no completed install file exists, and upgrade skip is false
- **THEN** the user is redirected to `/setup` and not to `/upgrade`

#### Scenario: Install skip bypasses setup

- **WHEN** `CAT2BUG_INSTALL_SKIP=true` or `cat2bug.install.skip=true`
- **THEN** the setup wizard is not required

### Requirement: Database configuration step validates connectivity

The setup wizard database step SHALL collect database type, connection parameters, and database name. The user MUST successfully test database connectivity before proceeding. The test result MUST display whether the database is `new` or `existing`.

#### Scenario: User cannot proceed without successful database test

- **WHEN** the user has not passed database connectivity test
- **THEN** the wizard does not allow advancing past the database step

#### Scenario: Test result shows attach mode

- **WHEN** database test succeeds for an existing H2 or MySQL database
- **THEN** the UI indicates that setup will attach to an existing database without running migrations during install

## ADDED Requirements

### Requirement: Setup submit carries database attach mode

The setup submit API SHALL accept `databaseMode` (`new` or `existing`) alongside database connection fields. The frontend MUST send the mode returned by the latest successful database test for the same connection parameters.

#### Scenario: Submit includes database mode from test

- **WHEN** the user completes setup after a successful database test returning `existing`
- **THEN** the submit payload includes `databaseMode: existing`
