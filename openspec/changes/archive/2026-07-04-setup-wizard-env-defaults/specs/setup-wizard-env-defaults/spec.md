## ADDED Requirements

### Requirement: Setup preflight API exposes environment-derived form defaults

The system SHALL provide an unauthenticated `GET /setup/preflight` endpoint while installation is not completed. The response MUST include wizard form fields derived from the current Spring `Environment` using the same property prefixes as `InstallConfigExporter` (`spring.database-type`, `spring.datasource.*`, `spring.redis.*`, `cat2bug.cache.*`, `cat2bug.profile`, `cat2bug.temp`, `cat2bug.ai.*`, `logging.file.*`). Fields without a corresponding environment value MUST be omitted or returned as null so the client can apply wizard defaults.

#### Scenario: Docker Compose MySQL and Redis variables are exported

- **WHEN** the application starts with `SPRING_DATASOURCE_DRUID_MASTER_URL` pointing at a MySQL JDBC URL, `SPRING_DATASOURCE_DRUID_MASTER_USERNAME`, `SPRING_DATASOURCE_DRUID_MASTER_PASSWORD`, and `SPRING_REDIS_HOST` set
- **THEN** `GET /setup/preflight` returns parsed `mysqlHost`, `mysqlPort`, `mysqlDatabase`, `mysqlUsername`, `mysqlPassword`, and `redisHost` matching those environment values

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

When `spring.database-type` is not set in the environment, the system SHALL infer `databaseType` as `mysql` if the exported datasource JDBC URL contains `jdbc:mysql`, otherwise the wizard default `h2` applies. When `cat2bug.cache.type` is not set, the system SHALL infer `cacheType` as `redis` if `spring.redis.host` is non-empty, otherwise the wizard default `local` applies.

#### Scenario: Infer MySQL from JDBC URL without SPRING_DATABASE_TYPE

- **WHEN** `SPRING_DATASOURCE_DRUID_MASTER_URL` is `jdbc:mysql://mysql:3306/cat2bug_platform` and `SPRING_DATABASE_TYPE` is unset
- **THEN** preflight sets `databaseType` to `mysql` and the wizard selects the MySQL database step defaults accordingly

#### Scenario: No inference without supporting signals

- **WHEN** no datasource URL, `spring.database-type`, or `spring.redis.host` is present in the environment
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
