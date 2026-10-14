## MODIFIED Requirements

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

### Requirement: Database and cache type inference when type env is missing

When neither `CAT2BUG_SETUP_DATABASE_TYPE` nor `SPRING_DATABASE_TYPE` is set, the system SHALL infer `databaseType` as `mysql` if the resolved datasource JDBC URL contains `jdbc:mysql`, otherwise the wizard default `h2` applies. When `CAT2BUG_CACHE_TYPE` is not set, the system SHALL infer `cacheType` as `redis` if a Redis host is provided, otherwise the wizard default `local` applies.

#### Scenario: Infer MySQL from JDBC URL without database type

- **WHEN** `CAT2BUG_SETUP_MYSQL_URL` is `jdbc:mysql://mysql:3306/cat2bug_platform` and no database type variable is set
- **THEN** preflight sets `databaseType` to `mysql` and the wizard selects the MySQL database step defaults accordingly

#### Scenario: No inference without supporting signals

- **WHEN** no datasource URL, database type or Redis host is present in the environment
- **THEN** preflight does not change `databaseType` or `cacheType` from wizard defaults (`h2`, `local`)
