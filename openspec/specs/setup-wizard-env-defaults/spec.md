# setup-wizard-env-defaults Specification

## Purpose
TBD - created by archiving change setup-wizard-env-defaults. Update Purpose after archive.
## Requirements
### Requirement: Setup preflight API exposes environment-derived form defaults

The system SHALL provide an unauthenticated `GET /setup/preflight` endpoint while installation is not completed. The response MUST include wizard form fields derived from environment variables and install configuration. Environment variables named `CAT2BUG_SETUP_*` (`CAT2BUG_SETUP_DATABASE_TYPE`, `CAT2BUG_SETUP_MYSQL_URL`, `CAT2BUG_SETUP_MYSQL_USERNAME`, `CAT2BUG_SETUP_MYSQL_PASSWORD`, `CAT2BUG_SETUP_REDIS_PORT`, `CAT2BUG_SETUP_REDIS_PASSWORD`, `CAT2BUG_SETUP_REDIS_DATABASE`) MUST take precedence; the legacy names `SPRING_DATABASE_TYPE`, `SPRING_DATASOURCE_DRUID_MASTER_*` (or `SPRING_DATASOURCE_URL`) and `SPRING_REDIS_*` MUST still be honored as fallbacks. Fields without a corresponding value MUST be omitted or returned as null so the client can apply wizard defaults.

#### Scenario: Docker Compose MySQL variables are exported

- **WHEN** the application starts with `CAT2BUG_SETUP_MYSQL_URL` pointing at a MySQL JDBC URL, `CAT2BUG_SETUP_MYSQL_USERNAME`, `CAT2BUG_SETUP_MYSQL_PASSWORD`, and `CAT2BUG_SETUP_REDIS_HOST` set
- **THEN** `GET /setup/preflight` returns parsed `mysqlHost`, `mysqlPort`, `mysqlDatabase`, `mysqlUsername`, `mysqlPassword`, and `redisHost` matching those environment values

#### Scenario: New names win over legacy names

- **WHEN** both `CAT2BUG_SETUP_MYSQL_URL` and `SPRING_DATASOURCE_DRUID_MASTER_URL` are set to different MySQL URLs
- **THEN** preflight parses host, port and database from `CAT2BUG_SETUP_MYSQL_URL`

#### Scenario: Legacy names still prefill

- **WHEN** only `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_DRUID_MASTER_USERNAME` and `SPRING_REDIS_HOST` are set
- **THEN** preflight returns the parsed MySQL fields, the username and `redisHost` from those variables

#### Scenario: Preflight unavailable after install completes

- **WHEN** `cat2bug.install.completed` is true on disk
- **THEN** `GET /setup/preflight` is rejected or returns an error consistent with other setup APIs after installation

### Requirement: Field merge priority is environment then wizard default then empty

The setup wizard frontend SHALL merge preflight values into the form using per-field priority: (1) non-empty environment-derived value from preflight, (2) existing hardcoded wizard default for that field, (3) empty string or false for booleans when neither applies. The UI MUST NOT display labels or badges indicating that values came from environment variables.

#### Scenario: Partial environment only overrides provided fields

- **WHEN** only `SPRING_REDIS_HOST` is set and MySQL-related environment variables are absent
- **THEN** the form shows the configured `redisHost` and retains existing wizard defaults for `mysqlHost`, `mysqlPort`, and other MySQL fields

#### Scenario: Password omitted when not in environment

- **WHEN** MySQL username is set via environment but password is not
- **THEN** `mysqlUsername` is prefilled and `mysqlPassword` remains empty (not a fabricated default)

### Requirement: Database and cache type inference when type env is missing

When neither `CAT2BUG_SETUP_DATABASE_TYPE` nor `SPRING_DATABASE_TYPE` is set, the system SHALL infer `databaseType` as `mysql` if the resolved datasource JDBC URL contains `jdbc:mysql`, otherwise the wizard default `h2` applies. When `CAT2BUG_CACHE_TYPE` is not set, the system SHALL infer `cacheType` as `redis` if a Redis host is provided, otherwise the wizard default `local` applies.

#### Scenario: Infer MySQL from JDBC URL without database type

- **WHEN** `CAT2BUG_SETUP_MYSQL_URL` is `jdbc:mysql://mysql:3306/cat2bug_platform` and no database type variable is set
- **THEN** preflight sets `databaseType` to `mysql` and the wizard selects the MySQL database step defaults accordingly

#### Scenario: No inference without supporting signals

- **WHEN** no datasource URL, database type or Redis host is present in the environment
- **THEN** preflight does not change `databaseType` or `cacheType` from wizard defaults (`h2`, `local`)

### Requirement: Setup preflight reuses established export and parsing logic

Backend implementation SHALL reuse `InstallConfigExporter.exportFromEnvironment` and the same JDBC URL and nested-property parsing used by upgrade preflight (`UpgradePreflightService` form field extraction) to avoid divergent host/port/database parsing between setup and upgrade flows.

#### Scenario: JDBC URL parsing matches upgrade preflight

- **WHEN** the same `SPRING_DATASOURCE_DRUID_MASTER_URL` is used for setup preflight and upgrade preflight on an uninitialized instance
- **THEN** parsed `mysqlHost`, `mysqlPort`, and `mysqlDatabase` are identical between the two endpoints

### Requirement: AI and storage fields follow the same merge rules

Ollama (`cat2bug.ai.enabled`, `cat2bug.ai.host`) and storage paths (`cat2bug.profile`, `cat2bug.temp`, `logging.file.path`) SHALL be included in preflight when present in the environment and merged into the setup form with the same environment-then-default-then-empty priority.

#### Scenario: AI host from environment

- **WHEN** `CAT2BUG_AI_HOST` (or equivalent relaxed binding for `cat2bug.ai.host`) is set to `http://ollama:11434`
- **THEN** the AI step shows `aiHost` as `http://ollama:11434` while `aiEnabled` follows environment if set, otherwise wizard default `false`

